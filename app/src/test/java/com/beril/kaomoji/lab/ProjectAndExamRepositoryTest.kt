package com.beril.kaomoji.lab

import com.beril.kaomoji.lab.model.ExamPayload
import com.beril.kaomoji.lab.model.ObjectKind
import com.beril.kaomoji.lab.model.ProjectPayload
import com.beril.kaomoji.lab.repository.LabRepository
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectAndExamRepositoryTest {

    @Test
    fun `creating a project keeps the research question fixed while notes evolve`() = runTest {
        val dao = FakeLabDao()
        val repo = LabRepository(dao)
        val id = repo.createProject("P1 — Hodgkin-Huxley", "İki nöron nasıl senkronize olur?")

        repo.updateProject(id, notes = "İlk simülasyon çalıştı.", nextAction = "Parametre taraması yap")

        val obj = dao.objects[id]!!
        assertEquals(ObjectKind.PROJECT, obj.kind)
        val p = ProjectPayload.fromJson(obj.payload)
        assertEquals("İki nöron nasıl senkronize olur?", p.researchQuestion)
        assertEquals("İlk simülasyon çalıştı.", p.notes)
        assertEquals("Parametre taraması yap", p.nextAction)
    }

    @Test
    fun `an exam with a date is immediately visible through the existing upcoming query`() = runTest {
        val dao = FakeLabDao()
        val repo = LabRepository(dao)
        val today = LocalDate.of(2027, 3, 1)
        repo.createExam("Fizik Ara Sınavı", "Elektromanyetizma", examDate = today.plusDays(5))

        val upcoming = repo.upcoming(days = 14, today = today)

        assertEquals(1, upcoming.size)
        assertEquals(ObjectKind.EXAM, upcoming.first().kind)
    }

    @Test
    fun `updating prep status only touches that field, scope is preserved`() = runTest {
        val dao = FakeLabDao()
        val repo = LabRepository(dao)
        val id = repo.createExam("Kimya Sınavı", "Asit-baz")

        repo.updateExamPrepStatus(id, "in-progress")

        val p = ExamPayload.fromJson(dao.objects[id]!!.payload)
        assertEquals("Asit-baz", p.scope)
        assertEquals("in-progress", p.prepStatus)
    }

    @Test
    fun `allExams returns both EXAM and ASSIGNMENT kinds, sorted by date`() = runTest {
        val dao = FakeLabDao()
        val repo = LabRepository(dao)
        val today = LocalDate.of(2027, 1, 1)
        repo.createExam("Ödev", "x", examDate = today.plusDays(10), isAssignment = true)
        repo.createExam("Sınav", "y", examDate = today.plusDays(2))

        val all = repo.allExams()

        assertEquals(2, all.size)
        assertTrue(all.first().title == "Sınav")
    }
}
