package com.beril.kaomoji.ai

import android.content.Context
import java.io.File

/** Seçili sağlayıcıya (Groq/Gemini) göre yönlendiren ince katman.
 *  UI ekranları GroqClient/GeminiClient'ı doğrudan çağırmak yerine bunu kullanır,
 *  böylece sağlayıcı değişimi tek yerden yönetilir. */
object AiClient {
    fun transcribeAudio(ctx: Context, apiKey: String?, audioFile: File): String =
        when (ApiKeyStore.provider(ctx)) {
            AiProvider.GEMINI -> GeminiClient.transcribeAudio(apiKey, audioFile)
            AiProvider.GROQ -> GroqClient.transcribeAudio(apiKey, audioFile)
        }

    fun analyzeTranscript(ctx: Context, apiKey: String?, transcript: String, topic: String?): String =
        when (ApiKeyStore.provider(ctx)) {
            AiProvider.GEMINI -> GeminiClient.analyzeTranscript(apiKey, transcript, topic)
            AiProvider.GROQ -> GroqClient.analyzeTranscript(apiKey, transcript, topic)
        }

    fun generateFlashcards(ctx: Context, apiKey: String?, sourceText: String, subjectName: String, n: Int = 8): List<Pair<String, String>> =
        when (ApiKeyStore.provider(ctx)) {
            AiProvider.GEMINI -> GeminiClient.generateFlashcards(apiKey, sourceText, subjectName, n)
            AiProvider.GROQ -> GroqClient.generateFlashcards(apiKey, sourceText, subjectName, n)
        }

    /** Yüklenen bir .md/.pdf belgesinden CurriculumLoader şemasına uygun ham JSON üretir
     *  (henüz doğrulanmamış — çağıran taraf CurriculumLoader.saveCustom ile doğrulayıp kaydeder). */
    fun generateCurriculum(ctx: Context, apiKey: String?, docTitle: String, docText: String): String =
        when (ApiKeyStore.provider(ctx)) {
            AiProvider.GEMINI -> GeminiClient.generateCurriculum(apiKey, docTitle, docText)
            AiProvider.GROQ -> GroqClient.generateCurriculum(apiKey, docTitle, docText)
        }
}
