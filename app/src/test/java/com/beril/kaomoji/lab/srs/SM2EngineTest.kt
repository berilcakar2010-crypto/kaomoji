package com.beril.kaomoji.lab.srs

import com.beril.kaomoji.lab.model.FlashcardPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SM2EngineTest {

    @Test
    fun `a failed recall (quality under 3) resets repetitions and interval to 1 day`() {
        val card = FlashcardPayload("Q", "A", easeFactor = 2.5, repetitions = 4, intervalDays = 20)
        val result = SM2Engine.review(card, quality = 1, todayEpochDay = 100)
        assertEquals(0, result.repetitions)
        assertEquals(1, result.intervalDays)
        assertEquals(101L, result.nextReviewEpochDay)
    }

    @Test
    fun `first two successful reviews use fixed intervals (1 day, then 6 days)`() {
        val card = FlashcardPayload("Q", "A")
        val first = SM2Engine.review(card, quality = 4, todayEpochDay = 0)
        assertEquals(1, first.repetitions)
        assertEquals(1, first.intervalDays)

        val second = SM2Engine.review(first, quality = 4, todayEpochDay = first.nextReviewEpochDay)
        assertEquals(2, second.repetitions)
        assertEquals(6, second.intervalDays)
    }

    @Test
    fun `ease factor never drops below 1_3 even on repeated failures`() {
        var card = FlashcardPayload("Q", "A", easeFactor = 1.3)
        repeat(5) { card = SM2Engine.review(card, quality = 0, todayEpochDay = 0) }
        assertTrue(card.easeFactor >= 1.3)
    }

    @Test
    fun `a good review after repetition 2 scales the interval by the ease factor`() {
        var card = FlashcardPayload("Q", "A", easeFactor = 2.0, repetitions = 2, intervalDays = 6)
        card = SM2Engine.review(card, quality = 4, todayEpochDay = 50)
        assertEquals(3, card.repetitions)
        assertEquals((6 * card.easeFactor).roundToIntForTest(), card.intervalDays)
    }

    private fun Double.roundToIntForTest(): Int = kotlin.math.round(this).toInt()
}
