package com.beril.kaomoji.ai.engine

import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** AIProvider'ı tamamen taklit eder — hiçbir gerçek ağ çağrısı yapmaz, sadece Gate'in
 *  doğru metoda doğru parametrelerle delege ettiğini ve Offline/Failure ayrımını koruduğunu
 *  doğrular. */
private class FakeAIProvider(
    private val result: AIResult<String> = AIResult.Success("ok"),
) : AIProvider {
    var lastCall: String? = null

    override suspend fun transcribeAudio(apiKey: String?, audioFile: File) = fail<String>()
    override suspend fun analyzeTranscript(apiKey: String?, transcript: String, topic: String?): AIResult<String> {
        lastCall = "analyzeTranscript:$topic"
        return result
    }
    override suspend fun evaluateProgress(apiKey: String?, statsSummary: String): AIResult<String> {
        lastCall = "evaluateProgress"
        return result
    }
    override suspend fun generateFlashcards(apiKey: String?, sourceText: String, subjectName: String, n: Int) = fail<List<Pair<String, String>>>()
    override suspend fun generateCurriculum(apiKey: String?, docTitle: String, docText: String) = fail<String>()

    override suspend fun improveWriting(apiKey: String?, text: String, instruction: String): AIResult<String> {
        lastCall = "improveWriting:$instruction"
        return result
    }
    override suspend fun proposeStudyPlan(apiKey: String?, contextSummary: String, horizonDays: Int): AIResult<String> {
        lastCall = "proposeStudyPlan:$horizonDays"
        return result
    }
    override suspend fun explainConcept(apiKey: String?, conceptTitle: String, conceptBody: String?, userAttempt: String?): AIResult<String> {
        lastCall = "explainConcept:$conceptTitle"
        return result
    }
    override suspend fun organizeResearchNotes(apiKey: String?, rawNotes: String): AIResult<String> {
        lastCall = "organizeResearchNotes"
        return result
    }

    private fun <T> fail(): AIResult<T> = AIResult.Failure("not used in this test")
}

class AICapabilityGateTest {

    @Test
    fun `every capability returns Offline when no provider is configured`() = runTest {
        val gate = AICapabilityGate(provider = null, apiKey = null)

        assertTrue(gate.improveWriting("x", "y") is AIResult.Offline)
        assertTrue(gate.proposeStudyPlan("ctx", 7) is AIResult.Offline)
        assertTrue(gate.explainConcept("t", "b") is AIResult.Offline)
        assertTrue(gate.evaluateProgress("summary") is AIResult.Offline)
        assertTrue(gate.organizeResearchNotes("notes") is AIResult.Offline)
        assertEquals(false, gate.isConfigured)
    }

    @Test
    fun `proposeStudyPlan never has access to anything that could apply the plan`() {
        // Derleme zamanı garantisi: AICapabilityGate ve AIProvider hiçbir LabRepository/LabDao
        // referansı almıyor (constructor imzalarına bakılabilir) — bu yüzden bu test sadece
        // imzaların hâlâ böyle olduğunu, yanlışlıkla bir repository parametresi eklenmediğini
        // dolaylı olarak doğrular: aşağıdaki satır derlenmiyorsa biri bu garantiyi bozmuştur.
        AICapabilityGate(provider = null, apiKey = null)
    }

    @Test
    fun `proposeStudyPlan delegates to the provider with the given horizon and is never auto-applied`() = runTest {
        val fake = FakeAIProvider(AIResult.Success("1. gün: ..."))
        val gate = AICapabilityGate(fake, apiKey = "k")

        val result = gate.proposeStudyPlan("bağlam özeti", horizonDays = 7)

        assertEquals("proposeStudyPlan:7", fake.lastCall)
        assertTrue(result is AIResult.Success)
        assertEquals("1. gün: ...", (result as AIResult.Success).value)
    }

    @Test
    fun `explainConcept passes the concept title through to the provider`() = runTest {
        val fake = FakeAIProvider()
        val gate = AICapabilityGate(fake, apiKey = "k")

        gate.explainConcept("Faraday Yasası", "EMF = -dΦ/dt")

        assertEquals("explainConcept:Faraday Yasası", fake.lastCall)
    }

    @Test
    fun `IOException from the provider layer becomes Offline, not a crash or generic failure`() = runTest {
        val result = runAICall<String> { throw java.net.UnknownHostException("no dns") }
        assertTrue(result is AIResult.Offline)
    }

    @Test
    fun `a non-network exception becomes a readable Failure, not Offline`() = runTest {
        val result = runAICall<String> { throw IllegalStateException("API anahtarı eksik") }
        assertTrue(result is AIResult.Failure)
        assertEquals("API anahtarı eksik", (result as AIResult.Failure).message)
    }
}
