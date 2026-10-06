package com.beril.kaomoji.lab

import com.beril.kaomoji.lab.model.ExplanationPayload
import com.beril.kaomoji.lab.model.KnowledgeObjectEntity
import com.beril.kaomoji.lab.model.ObjectKind
import com.beril.kaomoji.lab.model.RelationshipType
import com.beril.kaomoji.lab.model.Schedule
import com.beril.kaomoji.lab.model.ScheduleStatus
import com.beril.kaomoji.lab.repository.LabRepository
import java.time.Instant
import java.time.LocalDate
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
