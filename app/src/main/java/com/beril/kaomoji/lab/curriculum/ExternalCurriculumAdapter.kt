package com.beril.kaomoji.lab.curriculum

import com.beril.kaomoji.lab.db.LabDao
import com.beril.kaomoji.lab.model.ContextEntity
import com.beril.kaomoji.lab.model.ContextKind
import com.beril.kaomoji.lab.model.KnowledgeObjectEntity
import com.beril.kaomoji.lab.model.ObjectKind
import com.beril.kaomoji.lab.model.RelationshipEntity
import com.beril.kaomoji.lab.model.RelationshipType
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/**
 * Adaptör — "Lab 2.0 Personal Academic Curriculum" paketi (manifest/taxonomy/hierarchy/
 * objects/connections/contextMappings) için. Bu paket Contract v1'in (CurriculumPackage)
 * basit domains/courses/units/concepts/questions şeklinden daha zengin: uygun tipli önkoşul
 * kenarları (hard/soft/tool/intuition/co-requisite), disiplinlerarası bağlantılar, bilingual
 * başlıklar, yanlış kavramalar, ustalık kanıtı. Paketin kendi dokümantasyonu da "ince bir
 * adaptör yaz, içerik modüllerini değiştirme" diyor — bu dosya tam olarak o, Contract v1'i
 * değiştirmeden, Room şemasını değiştirmeden (mevcut `note` alanını kullanarak) yazıldı.
 *
 * Hiçbir alan sessizce atılmıyor: her öğrenme nesnesinin HAM JSON'u `payload`'da saklanır
 * (objectives/questions/misconceptions/masteryEvidence/derivation/purpose dahil) — bu adaptör
 * içerik üretmiyor, sadece yapıyı aktarıyor (§51).
 */
object ExternalCurriculumAdapter {

    suspend fun import(raw: String, dao: LabDao, packageId: String = "lab2-personal-curriculum") {
        val root = JSONObject(raw)
        val now = Instant.now()
        var objects = mutableListOf<KnowledgeObjectEntity>()
        val relationships = mutableListOf<RelationshipEntity>()
        val contexts = mutableListOf<ContextEntity>()

        // Ham geniş kategori etiketleri de (school/ap/olympiad/...) kendi Context satırlarını
        // alır, böylece bir nesnenin contextIds'i hem kaba (school) hem ince (ctx.school-...)
        // üyelikleri taşıyabilir.
        val broadContextIds = mutableSetOf<String>()

        // ── hierarchy: domain > course > unit — hepsi KnowledgeObjectEntity, kind=COURSE
        // (domain/course) ya da CURRICULUM_UNIT (unit); hangisi olduğu payload'da "level" ──
        val hierarchy = root.getJSONArray("hierarchy")
        for (i in 0 until hierarchy.length()) {
            val h = hierarchy.getJSONObject(i)
            val level = h.optString("level")
            val kind = if (level == "unit") ObjectKind.CURRICULUM_UNIT else ObjectKind.COURSE
            objects += KnowledgeObjectEntity(
                id = h.getString("id"),
                kind = kind,
                title = bilingualTitle(h.optJSONObject("title")) ?: h.getString("id"),
                body = h.optStringOrNull("description"),
                payload = h.toString(),
                createdAt = now, updatedAt = now,
                sourcePackageId = packageId,
            )
            h.optStringOrNull("parentId")?.let { parentId ->
                relationships += RelationshipEntity(fromId = h.getString("id"), toId = parentId, type = RelationshipType.USED_IN)
            }
        }

        // ── objects: öğrenme nesneleri — hepsi kind=CONCEPT, orijinal dış "kind" (skill/
        // equation/theorem/...) payload'ın içinde korunur ──
        val objArr = root.getJSONArray("objects")
        for (i in 0 until objArr.length()) {
            val o = objArr.getJSONObject(i)
            val id = o.getString("id")
            val contextTags = o.optJSONArray("contexts").orEmpty().strings().toSet()
            broadContextIds += contextTags

            objects += KnowledgeObjectEntity(
                id = id,
                kind = ObjectKind.CONCEPT,
                title = bilingualTitle(o.optJSONObject("title")) ?: id,
                body = o.optStringOrNull("description"),
                contextIds = contextTags,
                payload = o.toString(),
                createdAt = now, updatedAt = now,
                sourcePackageId = packageId,
            )

            o.optStringOrNull("parentId")?.let { parentId ->
                relationships += RelationshipEntity(fromId = id, toId = parentId, type = RelationshipType.USED_IN)
            }

            o.optJSONArray("prerequisites").orEmpty().let { arr ->
                for (j in 0 until arr.length()) {
                    val p = arr.getJSONObject(j)
                    relationships += RelationshipEntity(
                        fromId = p.getString("id"), toId = id,
                        type = RelationshipType.PREREQUISITE_OF,
                        note = p.optString("type", "hard"),
                    )
                }
            }
        }

        // ── connections: disiplinlerarası ilişkiler — tip + "why" `note`'a yazılır ──
        val connArr = root.getJSONArray("connections")
        for (i in 0 until connArr.length()) {
            val c = connArr.getJSONObject(i)
            relationships += RelationshipEntity(
                fromId = c.getString("from"), toId = c.getString("to"),
                type = RelationshipType.RELATED_TO,
                note = "${c.optString("type")}: ${c.optString("why")}",
            )
        }

        // ── broad context etiketleri (school/ap/olympiad/...) kendi Context satırı olur ──
        broadContextIds.forEach { tag -> contexts += ContextEntity(id = tag, name = tag, kind = contextKindFor(tag)) }

        // ── contextMappings: isimli alt-bağlamlar (örn. "Fizik olimpiyatı tarzı çalışma") —
        // kendi Context satırı + hedef nesnelerin contextIds'ine eklenir ──
        val ctxArr = root.getJSONArray("contextMappings")
        val extraContextIds = mutableMapOf<String, MutableSet<String>>()
        for (i in 0 until ctxArr.length()) {
            val m = ctxArr.getJSONObject(i)
            val mappingId = m.getString("id")
            contexts += ContextEntity(
                id = mappingId,
                name = bilingualTitle(m.optJSONObject("title")) ?: mappingId,
                kind = contextKindFor(m.optString("context")),
            )
            m.optJSONArray("targets").orEmpty().strings().forEach { targetId ->
                extraContextIds.getOrPut(targetId) { mutableSetOf() }.add(mappingId)
            }
        }
        if (extraContextIds.isNotEmpty()) {
            objects = objects.map { obj ->
                val extra = extraContextIds[obj.id] ?: return@map obj
                obj.copy(contextIds = obj.contextIds + extra)
            }.toMutableList()
        }

        dao.importPackage(objects, relationships, packageId)
        contexts.forEach { dao.upsertContext(it) }
    }

    private fun bilingualTitle(title: JSONObject?): String? {
        if (title == null) return null
        return title.optStringOrNull("tr") ?: title.optStringOrNull("en")
    }

    private fun contextKindFor(tag: String): ContextKind = when (tag) {
        "school" -> ContextKind.SCHOOL
        "ap" -> ContextKind.AP
        "olympiad" -> ContextKind.OLYMPIAD
        "independent" -> ContextKind.INDEPENDENT_STUDY
        "project" -> ContextKind.PROJECT
        "research" -> ContextKind.RESEARCH
        else -> ContextKind.OTHER
    }

    private fun JSONArray?.orEmpty(): JSONArray = this ?: JSONArray()
    private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }
    private fun JSONObject.optStringOrNull(key: String): String? = if (has(key) && !isNull(key)) getString(key) else null
}
