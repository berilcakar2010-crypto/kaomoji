package com.beril.kaomoji.lab

import com.beril.kaomoji.lab.model.KnowledgeObjectEntity
import com.beril.kaomoji.lab.model.ObjectKind
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
