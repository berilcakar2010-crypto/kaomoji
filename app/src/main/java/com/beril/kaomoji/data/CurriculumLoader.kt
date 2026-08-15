package com.beril.kaomoji.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object CurriculumLoader {

    fun load(ctx: Context): Curriculum {
        val raw = ctx.assets.open("curriculum.json").bufferedReader().use { it.readText() }
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
                                minutes = t.optInt("m", 30)
                            )
                        }
                    )
                }
            )
        }

        val projects = root.getJSONArray("projects").map { j ->
            val ph = j.getJSONObject("phases")
            val m = mutableMapOf<String, String>()
            ph.keys().forEach { k -> m[k] = ph.getString(k) }
            ProjectDef(
                id = j.getString("id"),
                name = j.getString("name"),
                emoji = j.optString("emoji", "🌱"),
                goal = j.getString("goal"),
                phaseWork = m,
                topics = j.optJSONArray("topics").strings(),
                defaultNext = j.optString("next", "")
            )
        }

        val assessments = root.getJSONArray("assessments").map { j ->
            AssessmentDef(
                id = j.getString("id"),
                name = j.getString("name"),
                scope = j.getString("scope"),
                hours = j.optInt("hours", 3),
                phaseId = j.getString("phase"),
                unitId = j.getString("unit")
            )
        }

        val bridges = root.getJSONArray("bridges").map { j ->
            Bridge(
                name = j.getString("n"),
                emoji = j.optString("e", "🔗"),
                desc = j.getString("d"),
                topics = j.optJSONArray("t").strings()
            )
        }

        val resources = root.getJSONArray("resources").map { j ->
            Resource(j.getString("s"), j.getString("n"), j.optString("u", ""))
        }

        val subjects = root.getJSONArray("subjects").map { j ->
            SubjectDef(j.getString("c"), j.getString("n"), j.getString("e"), j.getString("col"))
        }

        val kinds = root.getJSONArray("kinds").map { j ->
            KindDef(j.getString("c"), j.getString("n"), j.getString("e"))
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

    private fun JSONArray?.strings(): List<String> =
        if (this == null) emptyList() else (0 until length()).map { getString(it) }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) getString(key) else null
}
