package com.beril.kaomoji.lab.migration

import com.beril.kaomoji.data.AssessmentDef
import com.beril.kaomoji.data.AssessmentState
import com.beril.kaomoji.data.Flashcard
import com.beril.kaomoji.data.InboxNote
import com.beril.kaomoji.data.Mistake
import com.beril.kaomoji.data.ProblemLog
import com.beril.kaomoji.data.ProjectDef
import com.beril.kaomoji.data.ProjectState
import com.beril.kaomoji.data.Recording
import com.beril.kaomoji.data.WeeklyReview
import com.beril.kaomoji.lab.model.ExamPayload
import com.beril.kaomoji.lab.model.ExplanationPayload
import com.beril.kaomoji.lab.model.FlashcardPayload
import com.beril.kaomoji.lab.model.KnowledgeObjectEntity
import com.beril.kaomoji.lab.model.MistakePayload
import com.beril.kaomoji.lab.model.ObjectKind
import com.beril.kaomoji.lab.model.ProjectPayload
import com.beril.kaomoji.lab.model.Schedule
import com.beril.kaomoji.lab.model.ScheduleStatus
import java.time.Instant
import java.time.ZoneId

/**
 * Eski `Store.kt`'nin tuttuğu kullanıcı verisini (filesDir/state.json) Lab 2.0'ın bilgi
 * grafiğine çevirir. Store.kt'ye, eski ekranlara ya da eski verinin kendisine HİÇBİR ŞEKİLDE
 * dokunmaz — bu saf, tek yönlü bir OKUMA + yeni taraftaki yazma. Eski uygulama bu dosyadan
 * sonra da aynı şekilde çalışmayı sürdürür.
 *
 * Her fonksiyon saf (Context/I-O yok) — gerçek Store örneğini okuyup bu fonksiyonlara veri
 * geçiren ince sarmalayıcı [com.beril.kaomoji.lab.repository.LabRepository.migrateLegacyData]'da.
 *
 * DÜRÜST SINIRLAMA: eski `done`/`dailyLogs`/`problems` gibi bazı ince taneli istatistikler
 * (hangi görev ne zaman tamamlandı, günlük dakika sayıları) burada tek tek nesneye
 * çevrilmiyor — eski müfredatın görev id'leri yeni grafikteki hiçbir kavramla eşleşmiyor,
 * sahte bir bağlantı kurmak yerine bunlar şimdilik taşınmıyor. Taşınanlar: anlatımlar,
 * hatalar, tekrar kartları, Brain Inbox notları, proje/sınav durumları, pratik günlükleri,
 * haftalık değerlendirmeler — hepsi kendi içinde tam ve anlamlı kayıtlar.
 */
object LegacyDataMigrator {
    const val SOURCE_ID = "legacy-migration"

    fun mapMistake(m: Mistake): KnowledgeObjectEntity = KnowledgeObjectEntity(
        id = "legacy-mistake-${m.id}",
        kind = ObjectKind.MISTAKE,
        title = m.problem,
        payload = MistakePayload(
            problem = m.problem,
            attempt = "",
            whatWentWrong = m.why,
            whyItHappened = "",
            correctReasoning = m.correct,
            category = m.category,
        ).toJson(),
        createdAt = Instant.ofEpochMilli(m.createdAt),
        updatedAt = Instant.ofEpochMilli(m.createdAt),
        schedule = if (m.resolved) Schedule(status = ScheduleStatus.COMPLETED) else null,
        sourcePackageId = SOURCE_ID,
    )

    fun mapFlashcard(f: Flashcard): KnowledgeObjectEntity = KnowledgeObjectEntity(
        id = "legacy-flashcard-${f.id}",
        kind = ObjectKind.FLASHCARD,
        title = f.front,
        payload = FlashcardPayload(
            front = f.front,
            back = f.back,
            easeFactor = f.easeFactor.toDouble(),
            repetitions = f.repetitions,
            intervalDays = f.intervalDays,
            nextReviewEpochDay = if (f.nextReviewAt > 0) {
                Instant.ofEpochMilli(f.nextReviewAt).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
            } else 0L,
            lastQuality = f.lastQuality,
        ).toJson(),
        createdAt = Instant.ofEpochMilli(f.createdAt),
        updatedAt = Instant.ofEpochMilli(f.createdAt),
        sourcePackageId = SOURCE_ID,
    )

    fun mapRecording(r: Recording): KnowledgeObjectEntity = KnowledgeObjectEntity(
        id = "legacy-recording-${r.id}",
        kind = ObjectKind.EXPLANATION,
        title = r.title,
        body = r.transcript,
        payload = ExplanationPayload(
            language = r.language,
            audioFilePath = r.uri,
            confidence = null,
            aiEvaluation = r.analysis,
        ).toJson(),
        createdAt = Instant.ofEpochMilli(r.createdAt),
        updatedAt = Instant.ofEpochMilli(r.createdAt),
        sourcePackageId = SOURCE_ID,
    )

    fun mapInboxNote(n: InboxNote): KnowledgeObjectEntity = KnowledgeObjectEntity(
        id = "legacy-inbox-${n.id}",
        kind = ObjectKind.IDEA,
        title = n.text,
        body = n.category,
        createdAt = Instant.ofEpochMilli(n.createdAt),
        updatedAt = Instant.ofEpochMilli(n.createdAt),
        schedule = if (n.done) Schedule(status = ScheduleStatus.COMPLETED) else null,
        sourcePackageId = SOURCE_ID,
    )

    fun mapProblemLog(p: ProblemLog): KnowledgeObjectEntity = KnowledgeObjectEntity(
        id = "legacy-problem-${p.id}",
        kind = ObjectKind.PRACTICE,
        title = "${p.solved}/${p.attempted} çözüldü (${p.subject})",
        createdAt = Instant.ofEpochMilli(p.createdAt),
        updatedAt = Instant.ofEpochMilli(p.createdAt),
        sourcePackageId = SOURCE_ID,
    )

    fun mapWeeklyReview(w: WeeklyReview): KnowledgeObjectEntity = KnowledgeObjectEntity(
        id = "legacy-review-${w.id}",
        kind = ObjectKind.NOTE,
        title = "Haftalık Değerlendirme",
        body = listOf(
            "Üretilen: ${w.produced}",
            "Anlatabildiğim: ${w.canExplain}",
            "Hâlâ kitaba bakmam gereken: ${w.stillNeedsBook}",
        ).joinToString("\n"),
        createdAt = Instant.ofEpochMilli(w.createdAt),
        updatedAt = Instant.ofEpochMilli(w.createdAt),
        sourcePackageId = SOURCE_ID,
    )

    /** Araştırma sorusu olarak proje tanımının hedefi (goal) kullanılır — eski modelde ayrı
     *  bir "araştırma sorusu" alanı yoktu, goal en yakın karşılığı. */
    fun mapProject(def: ProjectDef, state: ProjectState): KnowledgeObjectEntity = KnowledgeObjectEntity(
        id = "legacy-project-${def.id}",
        kind = ObjectKind.PROJECT,
        title = def.name,
        payload = ProjectPayload(
            researchQuestion = def.goal,
            nextAction = state.nextAction.ifBlank { null },
            notes = state.notes,
        ).toJson(),
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        sourcePackageId = SOURCE_ID,
    )

    fun mapAssessment(def: AssessmentDef, state: AssessmentState): KnowledgeObjectEntity = KnowledgeObjectEntity(
        id = "legacy-assessment-${def.id}",
        kind = ObjectKind.EXAM,
        title = def.name,
        payload = ExamPayload(
            scope = def.scope,
            prepStatus = when {
                state.taken -> "ready"
                state.prepNotes.isNotBlank() -> "in-progress"
                else -> "not-started"
            },
        ).toJson(),
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        schedule = if (state.taken) Schedule(status = ScheduleStatus.COMPLETED) else null,
        sourcePackageId = SOURCE_ID,
    )
}

data class MigrationSummary(
    val mistakes: Int,
    val flashcards: Int,
    val recordings: Int,
    val inboxNotes: Int,
    val problems: Int,
    val reviews: Int,
    val projects: Int,
    val assessments: Int,
) {
    val total: Int get() = mistakes + flashcards + recordings + inboxNotes + problems + reviews + projects + assessments
}
