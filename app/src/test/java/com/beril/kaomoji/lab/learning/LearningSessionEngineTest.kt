package com.beril.kaomoji.lab.learning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningSessionEngineTest {

    @Test
    fun `every discipline starts with a question or encounter, never a passive reveal`() {
        LearningDiscipline.entries.forEach { d ->
            val first = LearningSessionEngine.stagesFor(d).first()
            assertTrue(
                "$d starts with $first, not question-first (§13)",
                first == LearningStage.QUESTION || first == LearningStage.ENCOUNTER,
            )
        }
    }

    @Test
    fun `math progression matches the spec example (question, attempt, struggle, derivation, variation, explain)`() {
        val stages = LearningSessionEngine.stagesFor(LearningDiscipline.MATH)
        assertEquals(
            listOf(
                LearningStage.QUESTION, LearningStage.ATTEMPT, LearningStage.STRUGGLE,
                LearningStage.DERIVATION, LearningStage.VARIATION, LearningStage.EXPLAIN_OWN_WORDS,
            ),
            stages,
        )
    }

    @Test
    fun `not every stage is required — disciplines have different lengths`() {
        val lengths = LearningDiscipline.entries.map { LearningSessionEngine.stagesFor(it).size }.toSet()
        assertTrue("expected disciplines to differ in required stages, got $lengths", lengths.size > 1)
    }

    @Test
    fun `advance moves forward one stage at a time, never skipping ahead`() {
        var state = LearningSessionState(LearningDiscipline.THEORY)
        val seen = mutableListOf(state.currentStage)
        while (!state.completed) {
            val before = state.stageIndex
            state = state.advance()
            if (!state.completed) {
                assertEquals(before + 1, state.stageIndex)
                seen += state.currentStage
            }
        }
        assertEquals(LearningSessionEngine.stagesFor(LearningDiscipline.THEORY), seen)
    }

    @Test
    fun `the user's attempt is preserved across stage transitions`() {
        var state = LearningSessionState(LearningDiscipline.MATH).withAttempt("Benim tahminim: F = ma")
        state = state.advance()
        assertEquals("Benim tahminim: F = ma", state.attemptText)
        assertFalse(state.completed)
    }

    @Test
    fun `completing the last stage marks the session completed without throwing`() {
        var state = LearningSessionState(LearningDiscipline.GENERAL)
        repeat(state.stages.size) { state = state.advance() }
        assertTrue(state.completed)
    }
}
