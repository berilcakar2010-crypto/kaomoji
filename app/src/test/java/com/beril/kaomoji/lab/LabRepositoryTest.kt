package com.beril.kaomoji.lab

import com.beril.kaomoji.lab.model.ExplanationPayload
import com.beril.kaomoji.lab.model.KnowledgeObjectEntity
import com.beril.kaomoji.lab.model.MistakePayload
import com.beril.kaomoji.lab.model.ObjectKind
import com.beril.kaomoji.lab.model.RelationshipType
import com.beril.kaomoji.lab.model.Schedule
import com.beril.kaomoji.lab.model.ScheduleStatus
import com.beril.kaomoji.lab.repository.LabRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LabRepositoryTest {

    @Test
    fun `createExplanation writes an EXPLANATION object linked to its concept via EXPLAINS`() = runTest {
        val dao = FakeLabDao()
        val repo = LabRepository(dao)

        val id = repo.createExplanation("concept-1", "Kablo Teorisi", "/data/recordings/a.m4a")

        val saved = dao.objects[id]
        assertEquals(ObjectKind.EXPLANATION, saved?.kind)
        assertEquals("/data/recordings/a.m4a", saved?.let { ExplanationPayload.fromJson(it.payload).audioFilePath })
        val rel = dao.relationships.values.single()
        assertEquals(RelationshipType.EXPLAINS, rel.type)
        assertEquals(id, rel.fromId)
        assertEquals("concept-1", rel.toId)
    }

    @Test
    fun `explanationsFor returns only this concept's recordings, newest first`() = runTest {
        val dao = FakeLabDao()
        val repo = LabRepository(dao)

        val older = repo.createExplanation("concept-1", "Kablo Teorisi", "/a.m4a")
        dao.objects[older] = dao.objects[older]!!.copy(createdAt = Instant.parse("2027-01-01T00:00:00Z"))
        val newer = repo.createExplanation("concept-1", "Kablo Teorisi", "/b.m4a")
        dao.objects[newer] = dao.objects[newer]!!.copy(createdAt = Instant.parse("2027-01-02T00:00:00Z"))
        repo.createExplanation("concept-2", "Başka Kavram", "/c.m4a")

        val result = repo.explanationsFor("concept-1").map { it.id }
        assertEquals(listOf(newer, older), result)
    }

    @Test
    fun `createVideoExplanation writes an EXPLANATION with videoFilePath, no audioFilePath`() = runTest {
        val dao = FakeLabDao()
        val repo = LabRepository(dao)

        val id = repo.createVideoExplanation("concept-1", "Kablo Teorisi", "/data/recordings/a.mp4")

        val saved = dao.objects[id]
        assertEquals(ObjectKind.EXPLANATION, saved?.kind)
        val payload = saved?.let { ExplanationPayload.fromJson(it.payload) }
        assertEquals("/data/recordings/a.mp4", payload?.videoFilePath)
        assertEquals(null, payload?.audioFilePath)
        val rel = dao.relationships.values.single()
        assertEquals(RelationshipType.EXPLAINS, rel.type)
        assertEquals(id, rel.fromId)
        assertEquals("concept-1", rel.toId)
    }

    @Test
    fun `allExplanations lists recordings across every concept, newest first, with concept titles`() = runTest {
        val dao = FakeLabDao()
        val repo = LabRepository(dao)

        val concept1 = repo.createConcept("Kablo Teorisi")
        val concept2 = repo.createConcept("Başka Kavram")
        val older = repo.createExplanation(concept1, "Kablo Teorisi", "/a.m4a")
        dao.objects[older] = dao.objects[older]!!.copy(createdAt = Instant.parse("2027-01-01T00:00:00Z"))
        val newer = repo.createExplanation(concept2, "Başka Kavram", "/b.m4a")
        dao.objects[newer] = dao.objects[newer]!!.copy(createdAt = Instant.parse("2027-01-02T00:00:00Z"))

        val result = repo.allExplanations()
        assertEquals(listOf(newer, older), result.map { it.explanation.id })
        assertEquals("Başka Kavram", result[0].conceptTitle)
        assertEquals(concept2, result[0].conceptId)
        assertEquals("Kablo Teorisi", result[1].conceptTitle)
    }

    @Test
    fun `statsSnapshot aggregates concepts, mistakes by category, flashcard ease, and streak`() = runTest {
        val dao = FakeLabDao()
        val repo = LabRepository(dao)

        repo.createConcept("Faraday Yasası")
        repo.createConcept("Lenz Yasası")
        repo.logMistake(MistakePayload("p1", "a1", "w1", "y1", "c1", "sign-error"))
        repo.logMistake(MistakePayload("p2", "a2", "w2", "y2", "c2", "sign-error"))
        repo.logMistake(MistakePayload("p3", "a3", "w3", "y3", "c3", "units"))
        val cardId = repo.createFlashcard("front", "back")

        // Bugün ve dün aktivite var, üç gün önce yok — seri 2 olmalı, 3 değil.
        val today = Instant.now()
        val yesterday = today.minus(java.time.Duration.ofDays(1))
        val threeDaysAgo = today.minus(java.time.Duration.ofDays(3))
        dao.objects[cardId] = dao.objects[cardId]!!.copy(updatedAt = yesterday)
        dao.objects.values.first { it.kind == ObjectKind.CONCEPT }.let {
            dao.objects[it.id] = it.copy(updatedAt = threeDaysAgo)
        }

        val stats = repo.statsSnapshot()

        assertEquals(2, stats.conceptCount)
        assertEquals(3, stats.mistakeCount)
        assertEquals("sign-error" to 2, stats.topMistakeCategories.first())
        assertEquals(1, stats.flashcardCount)
        assertEquals(2.5, stats.avgEaseFactor)
        assertEquals(2, stats.streakDays)
        assertTrue(stats.toSummary().contains("sign-error"))
    }

    @Test
    fun `attachTranscript saves the transcript as the explanation's body, leaves payload intact`() = runTest {
        val dao = FakeLabDao()
        val repo = LabRepository(dao)
        val id = repo.createExplanation("concept-1", "Kablo Teorisi", "/a.m4a")

        repo.attachTranscript(id, "Bugün kablo teorisini anlattım...")

        val saved = dao.objects[id]
        assertEquals("Bugün kablo teorisini anlattım...", saved?.body)
        assertEquals("/a.m4a", saved?.let { ExplanationPayload.fromJson(it.payload).audioFilePath })
    }

    @Test
    fun `attachEvaluation saves into the payload's aiEvaluation, leaves audioFilePath intact`() = runTest {
        val dao = FakeLabDao()
        val repo = LabRepository(dao)
        val id = repo.createExplanation("concept-1", "Kablo Teorisi", "/a.m4a")

        repo.attachEvaluation(id, "Temel kavramı doğru anlatmışsın, sınır koşullarını eksik bıraktın.")

        val payload = dao.objects[id]?.let { ExplanationPayload.fromJson(it.payload) }
        assertEquals("Temel kavramı doğru anlatmışsın, sınır koşullarını eksik bıraktın.", payload?.aiEvaluation)
        assertEquals("/a.m4a", payload?.audioFilePath)
    }

    @Test
    fun `quickCapture writes an IDEA object with no schedule, no forced classification`() = runTest {
        val dao = FakeLabDao()
        val repo = LabRepository(dao)

        val id = repo.quickCapture("Neden nöronlar senkronize oluyor?")

        val saved = dao.objects[id]
        assertEquals(ObjectKind.IDEA, saved?.kind)
        assertEquals(null, saved?.schedule)
    }

    @Test
    fun `missing a suggested target date shows up as a query result, not a punitive backlog state`() = runTest {
        val dao = FakeLabDao()
        val repo = LabRepository(dao)
        val today = LocalDate.of(2027, 3, 1)
        val yesterday = today.minusDays(1)

        dao.objects["late"] = KnowledgeObjectEntity(
            id = "late", kind = ObjectKind.CURRICULUM_UNIT, title = "Geciken birim",
            createdAt = Instant.now(), updatedAt = Instant.now(),
            schedule = Schedule(targetDate = yesterday.toEpochDay(), status = ScheduleStatus.TARGET_SET),
        )

        val result = repo.pastTargetDate(today)

        assertEquals(1, result.size)
        assertEquals("late", result.first().id)
        // Durum hâlâ TARGET_SET — sorgu onu COMPLETED/SKIPPED gibi bir "bozuldu" durumuna
        // zorlamıyor, sadece "bu bir seçenek" diye işaretliyor.
        assertTrue(result.first().schedule?.status == ScheduleStatus.TARGET_SET)
    }
}
