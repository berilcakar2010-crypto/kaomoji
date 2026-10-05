package com.beril.kaomoji.lab.migration

import com.beril.kaomoji.data.AssessmentDef
import com.beril.kaomoji.data.AssessmentState
import com.beril.kaomoji.data.Flashcard
import com.beril.kaomoji.data.InboxNote
import com.beril.kaomoji.data.Mistake
import com.beril.kaomoji.data.ProjectDef
import com.beril.kaomoji.data.ProjectState
import com.beril.kaomoji.data.Recording
import com.beril.kaomoji.lab.model.ExplanationPayload
import com.beril.kaomoji.lab.model.FlashcardPayload
import com.beril.kaomoji.lab.model.MistakePayload
import com.beril.kaomoji.lab.model.ObjectKind
import com.beril.kaomoji.lab.model.ProjectPayload
import com.beril.kaomoji.lab.model.ScheduleStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyDataMigratorTest {

    @Test
    fun `a resolved legacy mistake becomes COMPLETED, an unresolved one has no schedule`() {
        val resolved = Mistake(id = "m1", problem = "p", why = "w", correct = "c", category = "cat", subject = "s", createdAt = 1000L, resolved = true)
        val unresolved = resolved.copy(id = "m2", resolved = false)

        val r1 = LegacyDataMigrator.mapMistake(resolved)
        val r2 = LegacyDataMigrator.mapMistake(unresolved)

        assertEquals(ScheduleStatus.COMPLETED, r1.schedule?.status)
        assertNull(r2.schedule)
    }

    @Test
    fun `mistake field mapping preserves problem, why as whatWentWrong, correct as correctReasoning`() {
        val m = Mistake(id = "m3", problem = "Soru X", why = "İşaret hatası", correct = "Doğrusu şu", category = "sign-error", subject = "math", createdAt = 1000L)

        val obj = LegacyDataMigrator.mapMistake(m)
        val p = MistakePayload.fromJson(obj.payload)

        assertEquals("Soru X", p.problem)
        assertEquals("İşaret hatası", p.whatWentWrong)
        assertEquals("Doğrusu şu", p.correctReasoning)
        assertEquals("sign-error", p.category)
    }

    @Test
    fun `migrated ids are namespaced so they never collide with any other object`() {
        val m = Mistake(id = "x", problem = "p", why = "w", correct = "c", category = "cat", subject = "s", createdAt = 1L)
        assertTrue(LegacyDataMigrator.mapMistake(m).id.startsWith("legacy-mistake-"))
    }

    @Test
    fun `flashcard migration converts the millisecond due-date into an epoch day, never crashes on zero`() {
        val neverReviewed = Flashcard(id = "f1", front = "Q", back = "A", subject = "s", createdAt = 1000L, nextReviewAt = 0L)
        val reviewed = neverReviewed.copy(id = "f2", nextReviewAt = 1_700_000_000_000L)

        val r1 = LegacyDataMigrator.mapFlashcard(neverReviewed)
        val r2 = LegacyDataMigrator.mapFlashcard(reviewed)

        assertEquals(0L, FlashcardPayload.fromJson(r1.payload).nextReviewEpochDay)
        assertTrue(FlashcardPayload.fromJson(r2.payload).nextReviewEpochDay > 0L)
        assertEquals(ObjectKind.FLASHCARD, r2.kind)
    }

    @Test
    fun `a recording's transcript and analysis are preserved as body and aiEvaluation`() {
        val rec = Recording(id = "r1", title = "Anlatım", uri = "file://x", durationMs = 1000, createdAt = 1L, transcript = "metin", analysis = "değerlendirme")

        val obj = LegacyDataMigrator.mapRecording(rec)
        val p = ExplanationPayload.fromJson(obj.payload)

        assertEquals("metin", obj.body)
        assertEquals("değerlendirme", p.aiEvaluation)
        assertEquals("file://x", p.audioFilePath)
    }

    @Test
    fun `an inbox note marked done migrates as COMPLETED, otherwise no schedule`() {
        val done = InboxNote(id = "n1", text = "fikir", category = "Fikir", createdAt = 1L, done = true)
        val pending = done.copy(id = "n2", done = false)

        assertEquals(ScheduleStatus.COMPLETED, LegacyDataMigrator.mapInboxNote(done).schedule?.status)
        assertNull(LegacyDataMigrator.mapInboxNote(pending).schedule)
    }

    @Test
    fun `a legacy project's goal becomes the fixed research question, state becomes notes and next action`() {
        val def = ProjectDef(id = "P1", name = "P1 Project", emoji = "🧠", goal = "Soru?", phaseWork = emptyMap(), topics = emptyList(), defaultNext = "Başla")
        val state = ProjectState(nextAction = "Devam et", notes = "ilerleme notu")

        val obj = LegacyDataMigrator.mapProject(def, state)
        val p = ProjectPayload.fromJson(obj.payload)

        assertEquals(ObjectKind.PROJECT, obj.kind)
        assertEquals("Soru?", p.researchQuestion)
        assertEquals("Devam et", p.nextAction)
        assertEquals("ilerleme notu", p.notes)
    }

    @Test
    fun `a taken legacy assessment migrates as ready and COMPLETED`() {
        val def = AssessmentDef(id = "A1", name = "Sınav", scope = "Konular", hours = 2, phaseId = "faz", unitId = "unit")
        val taken = AssessmentState(taken = true)
        val notTaken = AssessmentState(taken = false, prepNotes = "biraz çalıştım")

        val r1 = LegacyDataMigrator.mapAssessment(def, taken)
        val r2 = LegacyDataMigrator.mapAssessment(def, notTaken)

        assertEquals(ScheduleStatus.COMPLETED, r1.schedule?.status)
        assertNull(r2.schedule)
    }
}
