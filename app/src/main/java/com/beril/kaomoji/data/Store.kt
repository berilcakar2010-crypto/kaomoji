package com.beril.kaomoji.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Single source of user state. Persisted as one JSON file in internal storage.
 * Small enough that a database would be overkill; keeps the build dependency-free.
 */
class Store(private val ctx: Context) {

    val curriculum: Curriculum = CurriculumLoader.load(ctx)

    // ── state ──
    val done = mutableStateMapOf<String, Boolean>()
    val recordings = mutableStateListOf<Recording>()
    val mistakes = mutableStateListOf<Mistake>()
    val inbox = mutableStateListOf<InboxNote>()
    val problems = mutableStateListOf<ProblemLog>()
    val reviews = mutableStateListOf<WeeklyReview>()
    val projectStates = mutableStateMapOf<String, ProjectState>()
    val assessmentStates = mutableStateMapOf<String, AssessmentState>()
    val skipped = mutableStateMapOf<String, Boolean>()

    var storageUri by mutableStateOf<String?>(null)
    var missionOverride by mutableStateOf<String?>(null)

    private val file: File get() = File(ctx.filesDir, "state.json")

    init {
        load()
        curriculum.projects.forEach { p ->
            if (projectStates[p.id] == null)
                projectStates[p.id] = ProjectState(nextAction = p.defaultNext)
        }
        curriculum.assessments.forEach { a ->
            if (assessmentStates[a.id] == null) assessmentStates[a.id] = AssessmentState()
        }
    }

    // ── derived: timeless progression ──

    /** Index of the first unit that is not fully complete. */
    val currentUnitIndex: Int
        get() {
            val units = curriculum.allUnits
            for (i in units.indices) if (!isUnitComplete(units[i])) return i
            return units.size - 1
        }

    val currentUnit: CurriculumUnit? get() = curriculum.allUnits.getOrNull(currentUnitIndex)
    val currentPhase: Phase? get() = currentUnit?.let { curriculum.phaseOf(it.id) }

    fun isUnitComplete(u: CurriculumUnit): Boolean =
        u.tasks.all { done[it.id] == true || skipped[it.id] == true }

    fun unitProgress(u: CurriculumUnit): Float {
        if (u.tasks.isEmpty()) return 1f
        val d = u.tasks.count { done[it.id] == true || skipped[it.id] == true }
        return d.toFloat() / u.tasks.size
    }

    /** A unit is unlocked once every earlier unit is complete. */
    fun isUnlocked(u: CurriculumUnit): Boolean = curriculum.unitIndexOf(u.id) <= currentUnitIndex

    fun phaseProgress(p: Phase): Float {
        val total = p.units.sumOf { it.tasks.size }
        if (total == 0) return 1f
        val d = p.units.sumOf { u -> u.tasks.count { done[it.id] == true || skipped[it.id] == true } }
        return d.toFloat() / total
    }

    val totalDone: Int get() = curriculum.allUnits.sumOf { u -> u.tasks.count { done[it.id] == true } }

    /** Garden growth stage 0..4 based on units completed. */
    val growthStage: Int
        get() {
            val ratio = currentUnitIndex.toFloat() / curriculum.allUnits.size.coerceAtLeast(1)
            return when {
                ratio < 0.08f -> 0
                ratio < 0.25f -> 1
                ratio < 0.50f -> 2
                ratio < 0.80f -> 3
                else -> 4
            }
        }

    fun subjectDone(code: String): Pair<Int, Int> {
        var d = 0; var t = 0
        curriculum.allUnits.forEach { u ->
            u.tasks.forEach { if (it.subject == code) { t++; if (done[it.id] == true) d++ } }
        }
        return d to t
    }

    // ── mutations ──

    fun toggleTask(id: String) {
        if (done[id] == true) done.remove(id) else done[id] = true
        skipped.remove(id)
        missionOverride = null
        save()
    }

    fun skipTask(id: String) {
        skipped[id] = true
        missionOverride = null
        save()
    }

    fun addRecording(r: Recording) { recordings.add(0, r); save() }
    fun updateRecording(r: Recording) {
        val i = recordings.indexOfFirst { it.id == r.id }
        if (i >= 0) recordings[i] = r
        save()
    }
    fun deleteRecording(id: String) { recordings.removeAll { it.id == id }; save() }

    fun addMistake(m: Mistake) { mistakes.add(0, m); save() }
    fun updateMistake(m: Mistake) {
        val i = mistakes.indexOfFirst { it.id == m.id }
        if (i >= 0) mistakes[i] = m
        save()
    }
    fun deleteMistake(id: String) { mistakes.removeAll { it.id == id }; save() }

    fun addInbox(text: String, cat: String) {
        inbox.add(0, InboxNote(uid(), text, cat, System.currentTimeMillis())); save()
    }
    fun updateInbox(n: InboxNote) {
        val i = inbox.indexOfFirst { it.id == n.id }
        if (i >= 0) inbox[i] = n
        save()
    }
    fun deleteInbox(id: String) { inbox.removeAll { it.id == id }; save() }

    fun logProblems(unitId: String?, subject: String, attempted: Int, solved: Int) {
        problems.add(0, ProblemLog(uid(), unitId, subject, attempted, solved, System.currentTimeMillis()))
        save()
    }

    fun addReview(r: WeeklyReview) { reviews.add(0, r); save() }

    fun setProject(id: String, s: ProjectState) { projectStates[id] = s; save() }
    fun setAssessment(id: String, s: AssessmentState) { assessmentStates[id] = s; save() }
    fun setStorage(uri: String?) { storageUri = uri; save() }

    val problemsSolved: Int get() = problems.sumOf { it.solved }
    val problemsAttempted: Int get() = problems.sumOf { it.attempted }

    /** Mistake categories that repeat 3+ times — surfaced as gentle patterns. */
    fun mistakePatterns(): List<Pair<String, Int>> =
        mistakes.filter { !it.resolved }
            .groupBy { it.subject }
            .map { (s, l) -> s to l.size }
            .filter { it.second >= 3 }
            .sortedByDescending { it.second }

    // ── persistence ──

    fun save() {
        try {
            val root = JSONObject()
            root.put("done", JSONArray(done.filterValues { it }.keys.toList()))
            root.put("skipped", JSONArray(skipped.filterValues { it }.keys.toList()))
            root.put("storageUri", storageUri ?: JSONObject.NULL)

            root.put("recordings", JSONArray().also { arr ->
                recordings.forEach { r ->
                    arr.put(JSONObject().apply {
                        put("id", r.id); put("title", r.title); put("uri", r.uri)
                        put("dur", r.durationMs); put("at", r.createdAt)
                        put("subject", r.subject ?: JSONObject.NULL)
                        put("unitId", r.unitId ?: JSONObject.NULL)
                        put("taskId", r.taskId ?: JSONObject.NULL)
                        put("projectId", r.projectId ?: JSONObject.NULL)
                        put("lang", r.language)
                        put("tags", JSONArray(r.tags))
                        put("fav", r.favorite); put("rev", r.needsReview)
                        put("pos", r.lastPositionMs); put("fey", r.isFeynman)
                    })
                }
            })

            root.put("mistakes", JSONArray().also { arr ->
                mistakes.forEach { m ->
                    arr.put(JSONObject().apply {
                        put("id", m.id); put("problem", m.problem); put("why", m.why)
                        put("correct", m.correct); put("cat", m.category); put("subject", m.subject)
                        put("unitId", m.unitId ?: JSONObject.NULL)
                        put("at", m.createdAt); put("res", m.resolved)
                    })
                }
            })

            root.put("inbox", JSONArray().also { arr ->
                inbox.forEach { n ->
                    arr.put(JSONObject().apply {
                        put("id", n.id); put("text", n.text); put("cat", n.category)
                        put("at", n.createdAt); put("done", n.done)
                    })
                }
            })

            root.put("problems", JSONArray().also { arr ->
                problems.forEach { p ->
                    arr.put(JSONObject().apply {
                        put("id", p.id); put("unitId", p.unitId ?: JSONObject.NULL)
                        put("subject", p.subject); put("a", p.attempted); put("s", p.solved)
                        put("at", p.createdAt)
                    })
                }
            })

            root.put("reviews", JSONArray().also { arr ->
                reviews.forEach { r ->
                    arr.put(JSONObject().apply {
                        put("id", r.id); put("produced", r.produced)
                        put("canExplain", r.canExplain); put("needsBook", r.stillNeedsBook)
                        put("declining", r.declining); put("at", r.createdAt)
                    })
                }
            })

            root.put("projects", JSONObject().also { o ->
                projectStates.forEach { (k, v) ->
                    o.put(k, JSONObject().apply {
                        put("next", v.nextAction); put("notes", v.notes)
                        put("ms", JSONArray(v.milestonesDone))
                    })
                }
            })

            root.put("assessments", JSONObject().also { o ->
                assessmentStates.forEach { (k, v) ->
                    o.put(k, JSONObject().apply {
                        put("prep", v.prepNotes); put("taken", v.taken)
                        put("score", v.score); put("rev", v.reviewDone)
                    })
                }
            })

            file.writeText(root.toString())
        } catch (_: Exception) {
        }
    }

    private fun load() {
        if (!file.exists()) return
        try {
            val root = JSONObject(file.readText())

            root.optJSONArray("done")?.let { a ->
                for (i in 0 until a.length()) done[a.getString(i)] = true
            }
            root.optJSONArray("skipped")?.let { a ->
                for (i in 0 until a.length()) skipped[a.getString(i)] = true
            }
            storageUri = if (root.isNull("storageUri")) null else root.optString("storageUri", "").ifBlank { null }

            root.optJSONArray("recordings")?.let { a ->
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    recordings.add(
                        Recording(
                            id = o.getString("id"),
                            title = o.getString("title"),
                            uri = o.getString("uri"),
                            durationMs = o.optLong("dur"),
                            createdAt = o.optLong("at"),
                            subject = o.optNull("subject"),
                            unitId = o.optNull("unitId"),
                            taskId = o.optNull("taskId"),
                            projectId = o.optNull("projectId"),
                            language = o.optString("lang", "EN"),
                            tags = o.optJSONArray("tags").toList(),
                            favorite = o.optBoolean("fav"),
                            needsReview = o.optBoolean("rev"),
                            lastPositionMs = o.optLong("pos"),
                            isFeynman = o.optBoolean("fey")
                        )
                    )
                }
            }

            root.optJSONArray("mistakes")?.let { a ->
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    mistakes.add(
                        Mistake(
                            id = o.getString("id"),
                            problem = o.getString("problem"),
                            why = o.optString("why", ""),
                            correct = o.optString("correct", ""),
                            category = o.optString("cat", "Kavram eksiği"),
                            subject = o.optString("subject", "phys"),
                            unitId = o.optNull("unitId"),
                            createdAt = o.optLong("at"),
                            resolved = o.optBoolean("res")
                        )
                    )
                }
            }

            root.optJSONArray("inbox")?.let { a ->
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    inbox.add(
                        InboxNote(
                            o.getString("id"), o.getString("text"),
                            o.optString("cat", "Fikir"), o.optLong("at"), o.optBoolean("done")
                        )
                    )
                }
            }

            root.optJSONArray("problems")?.let { a ->
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    problems.add(
                        ProblemLog(
                            o.getString("id"), o.optNull("unitId"), o.optString("subject", "phys"),
                            o.optInt("a"), o.optInt("s"), o.optLong("at")
                        )
                    )
                }
            }

            root.optJSONArray("reviews")?.let { a ->
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    reviews.add(
                        WeeklyReview(
                            o.getString("id"), o.optString("produced", ""),
                            o.optString("canExplain", ""), o.optString("needsBook", ""),
                            o.optBoolean("declining"), o.optLong("at")
                        )
                    )
                }
            }

            root.optJSONObject("projects")?.let { o ->
                o.keys().forEach { k ->
                    val j = o.getJSONObject(k)
                    projectStates[k] = ProjectState(
                        nextAction = j.optString("next", ""),
                        notes = j.optString("notes", ""),
                        milestonesDone = j.optJSONArray("ms").toList().toMutableList()
                    )
                }
            }

            root.optJSONObject("assessments")?.let { o ->
                o.keys().forEach { k ->
                    val j = o.getJSONObject(k)
                    assessmentStates[k] = AssessmentState(
                        prepNotes = j.optString("prep", ""),
                        taken = j.optBoolean("taken"),
                        score = j.optString("score", ""),
                        reviewDone = j.optBoolean("rev")
                    )
                }
            }
        } catch (_: Exception) {
        }
    }

    fun exportJson(): String = if (file.exists()) file.readText() else "{}"

    fun importJson(text: String): Boolean = try {
        JSONObject(text)
        file.writeText(text)
        true
    } catch (_: Exception) {
        false
    }

    fun resetAll() {
        done.clear(); skipped.clear(); recordings.clear(); mistakes.clear()
        inbox.clear(); problems.clear(); reviews.clear()
        projectStates.clear(); assessmentStates.clear()
        curriculum.projects.forEach { projectStates[it.id] = ProjectState(nextAction = it.defaultNext) }
        curriculum.assessments.forEach { assessmentStates[it.id] = AssessmentState() }
        save()
    }

    companion object {
        fun uid(): String = UUID.randomUUID().toString().take(12)
    }
}

private fun JSONObject.optNull(key: String): String? =
    if (has(key) && !isNull(key)) getString(key) else null

private fun JSONArray?.toList(): List<String> =
    if (this == null) emptyList() else (0 until length()).map { getString(it) }

fun uid(): String = Store.uid()
