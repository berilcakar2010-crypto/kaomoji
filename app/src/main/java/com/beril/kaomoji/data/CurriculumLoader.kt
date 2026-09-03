package com.beril.kaomoji.data

import android.content.Context
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/**
 * Müfredatı yükler. Önce kullanıcının (varsa) AI ile ürettiği özel müfredatı
 * (filesDir/custom_curriculum.json) dener, yoksa/bozuksa assets/curriculum.json'a
 * düşer. Bkz. ai/CurriculumGenClient.kt — üretim ve doğrulama burada, "parse".
 */
object CurriculumLoader {
    private const val CUSTOM_FILE = "custom_curriculum.json"

    fun load(ctx: Context): Curriculum {
        val custom = customFile(ctx)
        if (custom.exists()) {
            try {
                return parse(custom.readText())
            } catch (_: Exception) {
                // bozuk özel dosya — sessizce varsayılana düş, kullanıcının uygulaması kilitlenmesin
            }
        }
        val raw = ctx.assets.open("curriculum.json").bufferedReader().use { it.readText() }
        return parse(raw)
    }

    fun hasCustom(ctx: Context): Boolean = customFile(ctx).exists()

    fun clearCustom(ctx: Context): Boolean {
        val f = customFile(ctx)
        return if (f.exists()) f.delete() else true
    }

    /** AI'dan gelen ham JSON'u doğrular (parse edilemezse exception fırlatır, hiçbir şey
     *  yazılmaz) ve geçerliyse özel müfredat dosyası olarak kaydeder. */
    fun saveCustom(ctx: Context, rawJson: String) {
        val curriculum = parse(rawJson)
        require(curriculum.phases.isNotEmpty()) { "Müfredatta en az bir faz olmalı." }
        require(curriculum.allUnits.isNotEmpty()) { "Müfredatta en az bir birim olmalı." }
        customFile(ctx).writeText(rawJson)
    }

    private fun customFile(ctx: Context) = File(ctx.filesDir, CUSTOM_FILE)

    fun parse(raw: String): Curriculum {
        val root = JSONObject(raw)

        val phases = root.getJSONArray("phases").map { p ->
            Phase(
                id = p.getString("id"),
                name = p.getString("name"),
                sub = p.getString("sub"),
                goal = p.getString("goal"),
                hours = p.optInt("hours"),
                units = p.getJSONArray("units").map { u ->
                    CurriculumUnit(
                        id = u.getString("id"),
                        title = u.getString("title"),
                        kicker = u.getString("kicker"),
                        feynman = u.optStringOrNull("fey"),
                        note = u.optStringOrNull("note"),
                        bridges = u.optJSONArray("br").strings(),
                        tasks = u.getJSONArray("tasks").map { t ->
                            Task(
                                id = t.getString("i"),
                                text = t.getString("t"),
                                subject = t.getString("s"),
                                kind = t.getString("k"),
                                minutes = t.optInt("m", 30),
                                how = t.optJSONArray("h").strings(),
                                deliverable = t.optStringOrNull("o")
                            )
                        }
                    )
                }
            )
        }

        val projects = root.optJSONArray("projects").orEmpty().map { j ->
            val ph = j.optJSONObject("phases")
            val m = mutableMapOf<String, String>()
            ph?.keys()?.forEach { k -> m[k] = ph.getString(k) }
            ProjectDef(
                id = j.getString("id"),
                name = j.getString("name"),
                emoji = j.optString("emoji", "⚗️"),
                goal = j.optString("goal", ""),
                phaseWork = m,
                topics = j.optJSONArray("topics").strings(),
                defaultNext = j.optString("next", ""),
                milestones = j.optJSONArray("ms").strings()
            )
        }

        val assessments = root.optJSONArray("assessments").orEmpty().map { j ->
            AssessmentDef(
                id = j.getString("id"),
                name = j.getString("name"),
                scope = j.optString("scope", ""),
                hours = j.optInt("hours", 3),
                phaseId = j.getString("phase"),
                unitId = j.getString("unit")
            )
        }

        val bridges = root.optJSONArray("bridges").orEmpty().map { j ->
            Bridge(
                name = j.getString("n"),
                emoji = j.optString("e", "🔗"),
                desc = j.optString("d", ""),
                topics = j.optJSONArray("t").strings()
            )
        }

        val resources = root.optJSONArray("resources").orEmpty().map { j ->
            Resource(j.getString("s"), j.getString("n"), j.optString("u", ""))
        }

        val subjects = root.getJSONArray("subjects").map { j ->
            SubjectDef(j.getString("c"), j.getString("n"), j.optString("e", "📎"), j.optString("col", "#9D5CFF"))
        }

        val kinds = root.getJSONArray("kinds").map { j ->
            KindDef(j.getString("c"), j.getString("n"), j.optString("e", "🔹"))
        }

        return Curriculum(
            title = root.optString("title", "Müfredat"),
            phases = phases,
            projects = projects,
            assessments = assessments,
            bridges = bridges,
            resources = resources,
            subjects = subjects,
            kinds = kinds
        )
    }

    // ── small JSON helpers ──
    private inline fun <T> JSONArray.map(block: (JSONObject) -> T): List<T> =
        (0 until length()).map { block(getJSONObject(it)) }

    private fun JSONArray?.orEmpty(): JSONArray = this ?: JSONArray()

    private fun JSONArray?.strings(): List<String> =
        if (this == null) emptyList() else (0 until length()).map { getString(it) }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) getString(key) else null
}
