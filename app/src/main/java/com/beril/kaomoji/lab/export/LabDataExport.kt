package com.beril.kaomoji.lab.export

import com.beril.kaomoji.lab.model.ContextEntity
import com.beril.kaomoji.lab.model.ContextKind
import com.beril.kaomoji.lab.model.KnowledgeObjectEntity
import com.beril.kaomoji.lab.model.ObjectKind
import com.beril.kaomoji.lab.model.RelationshipEntity
import com.beril.kaomoji.lab.model.RelationshipType
import com.beril.kaomoji.lab.model.Schedule
import com.beril.kaomoji.lab.model.ScheduleStatus
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/**
 * Kullanıcının verisi kullanıcıya aittir (§34/§41). Bu uygulamanın ürettiği HER şeyi
 * (sunucu yok, vendor lock-in yok) tek bir insan-okunur JSON dosyasına dışa aktarır/geri
 * yükler. Curriculum Contract v1'den (§17, dış müfredat paketleri için) kasıtlı olarak ayrı
 * bir sözleşme — bu, kullanıcının KENDİ ürettiği verinin (ilerleme, hatalar, kartlar,
 * notlar) yedeği, bir müfredat paketi değil.
 */
object LabDataExport {
    private const val FORMAT_VERSION = 1

    fun export(
        objects: List<KnowledgeObjectEntity>,
        relationships: List<RelationshipEntity>,
        contexts: List<ContextEntity>,
    ): String {
        val root = JSONObject()
        root.put("formatVersion", FORMAT_VERSION)
        root.put("exportedAt", Instant.now().toString())

        val objArr = JSONArray()
        objects.forEach { o ->
            val j = JSONObject()
            j.put("id", o.id)
            j.put("kind", o.kind.name)
            j.put("title", o.title)
            o.body?.let { j.put("body", it) }
            j.put("contextIds", JSONArray(o.contextIds.toList()))
            j.put("createdAt", o.createdAt.toString())
            j.put("updatedAt", o.updatedAt.toString())
            o.schedule?.let { s ->
                val sj = JSONObject()
                s.suggestedDate?.let { sj.put("suggestedDate", it) }
                s.targetDate?.let { sj.put("targetDate", it) }
                s.deadline?.let { sj.put("deadline", it) }
                s.examDate?.let { sj.put("examDate", it) }
                s.priority?.let { sj.put("priority", it) }
                s.estimatedEffortMin?.let { sj.put("estimatedEffortMin", it) }
                sj.put("status", s.status.name)
                j.put("schedule", sj)
            }
            j.put("payload", o.payload)
            o.sourcePackageId?.let { j.put("sourcePackageId", it) }
            objArr.put(j)
        }
        root.put("objects", objArr)

        val relArr = JSONArray()
        relationships.forEach { r ->
            val j = JSONObject()
            j.put("id", r.id); j.put("fromId", r.fromId); j.put("toId", r.toId); j.put("type", r.type.name)
            r.note?.let { j.put("note", it) }
            relArr.put(j)
        }
        root.put("relationships", relArr)

        val ctxArr = JSONArray()
        contexts.forEach { c ->
            val j = JSONObject()
            j.put("id", c.id); j.put("name", c.name); j.put("kind", c.kind.name)
            ctxArr.put(j)
        }
        root.put("contexts", ctxArr)

        return root.toString(2)
    }

    data class ImportResult(
        val objects: List<KnowledgeObjectEntity>,
        val relationships: List<RelationshipEntity>,
        val contexts: List<ContextEntity>,
    )

    fun import(raw: String): ImportResult {
        val root = JSONObject(raw)
        val objects = root.getJSONArray("objects").let { arr ->
            (0 until arr.length()).map { i ->
                val j = arr.getJSONObject(i)
                val scheduleJson = j.optJSONObject("schedule")
                KnowledgeObjectEntity(
                    id = j.getString("id"),
                    kind = ObjectKind.valueOf(j.getString("kind")),
                    title = j.getString("title"),
                    body = if (j.has("body")) j.getString("body") else null,
                    contextIds = j.optJSONArray("contextIds").let { ca ->
                        if (ca == null) emptySet() else (0 until ca.length()).map { ca.getString(it) }.toSet()
                    },
                    createdAt = Instant.parse(j.getString("createdAt")),
                    updatedAt = Instant.parse(j.getString("updatedAt")),
                    schedule = scheduleJson?.let { sj ->
                        Schedule(
                            suggestedDate = if (sj.has("suggestedDate")) sj.getLong("suggestedDate") else null,
                            targetDate = if (sj.has("targetDate")) sj.getLong("targetDate") else null,
                            deadline = if (sj.has("deadline")) sj.getLong("deadline") else null,
                            examDate = if (sj.has("examDate")) sj.getLong("examDate") else null,
                            priority = if (sj.has("priority")) sj.getInt("priority") else null,
                            estimatedEffortMin = if (sj.has("estimatedEffortMin")) sj.getInt("estimatedEffortMin") else null,
                            status = ScheduleStatus.valueOf(sj.optString("status", ScheduleStatus.SUGGESTED.name)),
                        )
                    },
                    payload = j.optString("payload", "{}"),
                    sourcePackageId = if (j.has("sourcePackageId")) j.getString("sourcePackageId") else null,
                )
            }
        }

        val relationships = root.getJSONArray("relationships").let { arr ->
            (0 until arr.length()).map { i ->
                val j = arr.getJSONObject(i)
                RelationshipEntity(
                    id = j.getString("id"),
                    fromId = j.getString("fromId"),
                    toId = j.getString("toId"),
                    type = RelationshipType.valueOf(j.getString("type")),
                    note = if (j.has("note")) j.getString("note") else null,
                )
            }
        }

        val contexts = root.getJSONArray("contexts").let { arr ->
            (0 until arr.length()).map { i ->
                val j = arr.getJSONObject(i)
                ContextEntity(id = j.getString("id"), name = j.getString("name"), kind = ContextKind.valueOf(j.getString("kind")))
            }
        }

        return ImportResult(objects, relationships, contexts)
    }
}
