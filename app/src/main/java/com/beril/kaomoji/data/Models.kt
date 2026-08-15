package com.beril.kaomoji.data

// ── Curriculum (read-only, loaded from assets/curriculum.json) ──────────

data class Task(
    val id: String,
    val text: String,
    val subject: String,
    val kind: String,
    val minutes: Int
)

data class CurriculumUnit(
    val id: String,
    val title: String,
    val kicker: String,
    val tasks: List<Task>,
    val feynman: String? = null,
    val bridges: List<String> = emptyList(),
    val note: String? = null
)

data class Phase(
    val id: String,
    val name: String,
    val sub: String,
    val goal: String,
    val hours: Int,
    val units: List<CurriculumUnit>
)

data class ProjectDef(
    val id: String,
    val name: String,
    val emoji: String,
    val goal: String,
    val phaseWork: Map<String, String>,
    val topics: List<String>,
    val defaultNext: String
)

data class AssessmentDef(
    val id: String,
    val name: String,
    val scope: String,
    val hours: Int,
    val phaseId: String,
    val unitId: String
)

data class Bridge(
    val name: String,
    val emoji: String,
    val desc: String,
    val topics: List<String>
)

data class Resource(val subject: String, val name: String, val use: String)

data class SubjectDef(val code: String, val name: String, val emoji: String, val color: String)

data class KindDef(val code: String, val name: String, val emoji: String)

data class Curriculum(
    val title: String,
    val phases: List<Phase>,
    val projects: List<ProjectDef>,
    val assessments: List<AssessmentDef>,
    val bridges: List<Bridge>,
    val resources: List<Resource>,
    val subjects: List<SubjectDef>,
    val kinds: List<KindDef>
) {
    val allUnits: List<CurriculumUnit> = phases.flatMap { it.units }
    private val unitIndex = allUnits.withIndex().associate { (i, u) -> u.id to i }
    private val taskMap = allUnits.flatMap { it.tasks }.associateBy { it.id }
    private val unitOfTask = allUnits.flatMap { u -> u.tasks.map { it.id to u } }.toMap()
    private val phaseOfUnit = phases.flatMap { p -> p.units.map { it.id to p } }.toMap()

    fun unitIndexOf(unitId: String): Int = unitIndex[unitId] ?: -1
    fun unitAt(i: Int): CurriculumUnit? = allUnits.getOrNull(i)
    fun task(id: String): Task? = taskMap[id]
    fun unitOf(taskId: String): CurriculumUnit? = unitOfTask[taskId]
    fun phaseOf(unitId: String): Phase? = phaseOfUnit[unitId]
    fun subject(code: String): SubjectDef? = subjects.firstOrNull { it.code == code }
    fun kind(code: String): KindDef? = kinds.firstOrNull { it.code == code }
    val totalTasks: Int = allUnits.sumOf { it.tasks.size }
}

// ── User data (mutable, persisted as JSON) ──────────────────────────────

data class Recording(
    val id: String,
    var title: String,
    var uri: String,
    var durationMs: Long,
    var createdAt: Long,
    var subject: String? = null,
    var unitId: String? = null,
    var taskId: String? = null,
    var projectId: String? = null,
    var language: String = "EN",
    var tags: List<String> = emptyList(),
    var favorite: Boolean = false,
    var needsReview: Boolean = false,
    var lastPositionMs: Long = 0L,
    var isFeynman: Boolean = false
)

data class Mistake(
    val id: String,
    var problem: String,
    var why: String,
    var correct: String,
    var category: String,
    var subject: String,
    var unitId: String? = null,
    var createdAt: Long,
    var resolved: Boolean = false
)

data class InboxNote(
    val id: String,
    var text: String,
    var category: String,
    var createdAt: Long,
    var done: Boolean = false
)

data class ProblemLog(
    val id: String,
    var unitId: String?,
    var subject: String,
    var attempted: Int,
    var solved: Int,
    var createdAt: Long
)

data class AssessmentState(
    var prepNotes: String = "",
    var taken: Boolean = false,
    var score: String = "",
    var reviewDone: Boolean = false
)

data class WeeklyReview(
    val id: String,
    var produced: String,
    var canExplain: String,
    var stillNeedsBook: String,
    var declining: Boolean,
    var createdAt: Long
)

data class ProjectState(
    var nextAction: String = "",
    var notes: String = "",
    var milestonesDone: MutableList<String> = mutableListOf()
)

object Categories {
    val mistake = listOf(
        "Kavram eksiği", "Teknik hata", "Dikkatsizlik", "Cebir",
        "Hesap", "Soruyu yanlış anladım", "Yetersiz pratik"
    )
    val inbox = listOf(
        "Akademik", "Araştırma", "Kod", "Japonca", "Almanca",
        "Fikir", "Proje", "Soru", "Sonra oku"
    )
}
