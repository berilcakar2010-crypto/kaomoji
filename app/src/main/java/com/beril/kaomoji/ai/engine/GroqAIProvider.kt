package com.beril.kaomoji.ai.engine

import com.beril.kaomoji.ai.GroqClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** [AIProvider] → Groq. GroqClient'ın bloklayan ağ çağrılarını Dispatchers.IO'da çalıştırıp
 *  sonucu [AIResult]'a sarar — çağıran taraf artık ne iş parçacığını ne de try/catch'i
 *  kendisi düşünmek zorunda. */
class GroqAIProvider : AIProvider {
    override suspend fun transcribeAudio(apiKey: String?, audioFile: File): AIResult<String> =
        withContext(Dispatchers.IO) { runAICall { GroqClient.transcribeAudio(apiKey, audioFile) } }

    override suspend fun analyzeTranscript(apiKey: String?, transcript: String, topic: String?): AIResult<String> =
        withContext(Dispatchers.IO) { runAICall { GroqClient.analyzeTranscript(apiKey, transcript, topic) } }

    override suspend fun evaluateProgress(apiKey: String?, statsSummary: String): AIResult<String> =
        withContext(Dispatchers.IO) { runAICall { GroqClient.evaluateProgress(apiKey, statsSummary) } }

    override suspend fun generateFlashcards(apiKey: String?, sourceText: String, subjectName: String, n: Int): AIResult<List<Pair<String, String>>> =
        withContext(Dispatchers.IO) { runAICall { GroqClient.generateFlashcards(apiKey, sourceText, subjectName, n) } }

    override suspend fun generateCurriculum(apiKey: String?, docTitle: String, docText: String): AIResult<String> =
        withContext(Dispatchers.IO) { runAICall { GroqClient.generateCurriculum(apiKey, docTitle, docText) } }

    override suspend fun improveWriting(apiKey: String?, text: String, instruction: String): AIResult<String> =
        withContext(Dispatchers.IO) {
            runAICall {
                GroqClient.analyzeTranscript(
                    apiKey,
                    transcript = text,
                    topic = "YAZIM YARDIMI — sadece şunu yap: $instruction. Anlamı değiştirme, " +
                        "sadece netlik/dilbilgisi/yapı öner. Düzeltilmiş metni döndür.",
                )
            }
        }

    override suspend fun proposeStudyPlan(apiKey: String?, contextSummary: String, horizonDays: Int): AIResult<String> =
        withContext(Dispatchers.IO) {
            runAICall {
                GroqClient.evaluateProgress(
                    apiKey,
                    "PLAN ÖNERİSİ İSTEĞİ (sadece öneri, otomatik uygulanmayacak). " +
                        "Önümüzdeki $horizonDays gün için bir çalışma planı öner.\n\n$contextSummary",
                )
            }
        }

    override suspend fun explainConcept(apiKey: String?, conceptTitle: String, conceptBody: String?, userAttempt: String?): AIResult<String> =
        withContext(Dispatchers.IO) {
            runAICall {
                val attemptPart = if (userAttempt.isNullOrBlank()) "" else "\n\nKullanıcının kendi tahmini/denemesi: $userAttempt"
                GroqClient.analyzeTranscript(
                    apiKey,
                    transcript = "Kavram: $conceptTitle\n${conceptBody.orEmpty()}$attemptPart",
                    topic = "KAVRAM AÇIKLAMA İSTEĞİ — kullanıcının kendi denemesi varsa önce onu değerlendir, " +
                        "sonra sadece ihtiyacı olan kısmı açıkla (her şeyi tek seferde dökme).",
                )
            }
        }

    override suspend fun organizeResearchNotes(apiKey: String?, rawNotes: String): AIResult<String> =
        withContext(Dispatchers.IO) {
            runAICall {
                GroqClient.analyzeTranscript(
                    apiKey,
                    transcript = rawNotes,
                    topic = "NOT DÜZENLEME İSTEĞİ — sadece verilen notları düzenle/özetle/bağlantı öner. " +
                        "Kaynak, deney veya sonuç uydurma; sadece verilen metinle çalış.",
                )
            }
        }
}
