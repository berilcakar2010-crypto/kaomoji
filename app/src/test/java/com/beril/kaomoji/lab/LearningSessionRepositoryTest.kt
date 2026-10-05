package com.beril.kaomoji.lab

import com.beril.kaomoji.lab.learning.LearningDiscipline
import com.beril.kaomoji.lab.model.ObjectKind
import com.beril.kaomoji.lab.model.RelationshipType
import com.beril.kaomoji.lab.repository.LabRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningSessionRepositoryTest {

    @Test
    fun `starting a session links it to its concept with REINFORCES and preserves an empty attempt`() = runTest {
        val dao = FakeLabDao()
        val repo = LabRepository(dao)
        val conceptId = repo.createConcept("Faraday Yasası", "EMF = -dΦ/dt")

        val sessionId = repo.startLearningSession(conceptId, LearningDiscipline.PHYSICS)

        assertEquals(ObjectKind.LEARNING_SESSION, dao.objects[sessionId]?.kind)
        val rel = dao.relationships.values.find { it.fromId == sessionId && it.toId == conceptId }
        assertNotNull(rel)
        assertEquals(RelationshipType.REINFORCES, rel!!.type)

        val state = repo.getLearningSessionState(sessionId)
        assertNotNull(state)
        assertEquals(LearningDiscipline.PHYSICS, state!!.discipline)
        assertEquals("", state.attemptText)
        assertTrue(!state.completed)
    }

    @Test
    fun `saving session state round-trips the attempt and stage index`() = runTest {
        val dao = FakeLabDao()
        val repo = LabRepository(dao)
        val conceptId = repo.createConcept("Cable theory")
        val sessionId = repo.startLearningSession(conceptId, LearningDiscipline.MATH)

        var state = repo.getLearningSessionState(sessionId)!!.withAttempt("V = IR sanırım")
        state = state.advance()
        repo.saveLearningSessionState(sessionId, state)

        val reloaded = repo.getLearningSessionState(sessionId)!!
        assertEquals("V = IR sanırım", reloaded.attemptText)
        assertEquals(1, reloaded.stageIndex)
    }
}
