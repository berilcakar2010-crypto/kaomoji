package com.beril.kaomoji.lab

import com.beril.kaomoji.lab.model.FlashcardPayload
import com.beril.kaomoji.lab.model.MistakePayload
import com.beril.kaomoji.lab.model.ObjectKind
import com.beril.kaomoji.lab.model.RelationshipType
import com.beril.kaomoji.lab.repository.LabRepository
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MistakeAndFlashcardRepositoryTest {

    @Test
    fun `logging a mistake with a concept links it via CAUSED_BY, not a vague tag`() = runTest {
        val dao = FakeLabDao()
        val repo = LabRepository(dao)
        val conceptId = repo.createConcept("Vektörler")

        val mistakeId = repo.logMistake(
            MistakePayload("Problem", "Denemem", "İşaret hatası", "Dikkatsizlik", "Doğrusu şöyle", "sign-error"),
            conceptId = conceptId,
        )

        assertEquals(ObjectKind.MISTAKE, dao.objects[mistakeId]?.kind)
        val rel = dao.relationships.values.find { it.fromId == conceptId && it.toId == mistakeId }
        assertEquals(RelationshipType.CAUSED_BY, rel?.type)
    }

    @Test
    fun `mistakeCategoryCounts surfaces recurring patterns without discarding any mistake`() = runTest {
        val dao = FakeLabDao()
        val repo = LabRepository(dao)
        repeat(3) { repo.logMistake(MistakePayload("p$it", "a", "w", "y", "c", "sign-error")) }
        repo.logMistake(MistakePayload("p4", "a", "w", "y", "c", "algebra-slip"))

        val counts = repo.mistakeCategoryCounts()

        assertEquals(3, counts["sign-error"])
        assertEquals(1, counts["algebra-slip"])
    }

    @Test
    fun `a brand new flashcard is immediately due (nextReviewEpochDay defaults to 0)`() = runTest {
        val dao = FakeLabDao()
        val repo = LabRepository(dao)
        repo.createFlashcard("Soru", "Cevap")

        val due = repo.dueFlashcards(LocalDate.of(2027, 1, 1))

        assertEquals(1, due.size)
    }

    @Test
    fun `reviewing a flashcard persists SM-2 state and pushes it out of the due list for a well-recalled card`() = runTest {
        val dao = FakeLabDao()
        val repo = LabRepository(dao)
        val id = repo.createFlashcard("Soru", "Cevap")
        val today = LocalDate.of(2027, 1, 1)

        repo.reviewFlashcard(id, quality = 4, today = today)

        val payload = FlashcardPayload.fromJson(dao.objects[id]!!.payload)
        assertEquals(1, payload.repetitions)
        assertTrue(payload.nextReviewEpochDay > today.toEpochDay())

        val stillDueToday = repo.dueFlashcards(today)
        assertTrue(stillDueToday.isEmpty())
    }
}
