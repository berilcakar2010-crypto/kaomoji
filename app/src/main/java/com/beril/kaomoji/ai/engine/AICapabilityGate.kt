package com.beril.kaomoji.ai.engine

import android.content.Context
import com.beril.kaomoji.ai.ApiKeyStore
import com.beril.kaomoji.ai.AiProvider
import java.io.File

/**
 * Yetki sınırlı AI katmanı (§5). Hiçbir çağıran taraf "genel bir AI asistanı" çağırmaz —
 * sadece burada adlandırılmış, dar kapsamlı yeteneklerden birini çağırabilir. Bu sınıfın
 * [com.beril.kaomoji.lab.repository.LabRepository] ya da herhangi bir veri yazma yoluna
 * hiçbir referansı YOK — yani `proposeStudyPlan` ne kadar "ikna edici" bir plan önerse de,
 * bunu kullanıcı adına uygulamaya fiziksel olarak yetkisi yok. Planı gerçekten uygulamak
 * (bir KnowledgeObjectEntity'nin Schedule'ını değiştirmek) her zaman çağıran ekranın,
 * kullanıcı onayından SONRA, ayrı bir repository çağrısıyla yapması gereken bir şey.
 *
 * `provider == null` (anahtar girilmemiş) durumunda her metot senkron olarak
 * [AIResult.Offline] döner — hiçbir istek sıraya girmez, hiçbir şey sessizce ağa çıkmaz (§35).
 */
class AICapabilityGate(private val provider: AIProvider?, private val apiKey: String?) {

    /** Yazma yardımı: ifade/dilbilgisi/yapı. Anlamı korur. Planı ya da akademik veriyi
     *  ASLA değiştirmez — sadece önerilen metni döndürür. */
    suspend fun improveWriting(text: String, instruction: String): AIResult<String> =
        provider?.improveWriting(apiKey, text, instruction) ?: AIResult.Offline

    /** Planlama: sadece ÖNERİ üretir (Mod C — §4). Çağıran taraf kullanıcıya göstermeden
     *  veya onay almadan sonucu bir Schedule'a yazmamalı; bu sınıf bunu yapamaz zaten. */
    suspend fun proposeStudyPlan(contextSummary: String, horizonDays: Int): AIResult<String> =
        provider?.proposeStudyPlan(apiKey, contextSummary, horizonDays) ?: AIResult.Offline

    /** Akademik içerik: açıklar, sorgular, muhakemedeki boşlukları işaret eder. Kullanıcının
     *  kendi akıl yürütmesinin yerine geçmez — soru-önce akışının bir adımı olarak kullanılmalı. */
    suspend fun explainConcept(conceptTitle: String, conceptBody: String?, userAttempt: String? = null): AIResult<String> =
        provider?.explainConcept(apiKey, conceptTitle, conceptBody, userAttempt) ?: AIResult.Offline

    /** Akademik içerik: ilerleme verisinden yoğunlaştırılmış, veri odaklı bir durum değerlendirmesi. */
    suspend fun evaluateProgress(statsSummary: String): AIResult<String> =
        provider?.evaluateProgress(apiKey, statsSummary) ?: AIResult.Offline

    /** Araştırma: verilen notları düzenler/özetler/bağlantı önerir. Kaynak, deney veya sonuç
     *  UYDURMAZ — sadece sağlanan metinle çalışır (§5 Research). */
    suspend fun organizeResearchNotes(rawNotes: String): AIResult<String> =
        provider?.organizeResearchNotes(apiKey, rawNotes) ?: AIResult.Offline

    suspend fun analyzeTranscript(transcript: String, topic: String?): AIResult<String> =
        provider?.analyzeTranscript(apiKey, transcript, topic) ?: AIResult.Offline

    suspend fun transcribeAudio(audioFile: File): AIResult<String> =
        provider?.transcribeAudio(apiKey, audioFile) ?: AIResult.Offline

    suspend fun generateFlashcards(sourceText: String, subjectName: String, n: Int = 8): AIResult<List<Pair<String, String>>> =
        provider?.generateFlashcards(apiKey, sourceText, subjectName, n) ?: AIResult.Offline

    suspend fun generateCurriculum(docTitle: String, docText: String): AIResult<String> =
        provider?.generateCurriculum(apiKey, docTitle, docText) ?: AIResult.Offline

    val isConfigured: Boolean get() = provider != null

    companion object {
        /** Kullanıcının Ayarlar'da seçtiği sağlayıcı + o sağlayıcının anahtarına göre kapıyı
         *  kurar. Anahtar yoksa `provider = null` — kapı hep var, sadece hep Offline döner. */
        fun forContext(ctx: Context): AICapabilityGate {
            val providerId = ApiKeyStore.provider(ctx)
            val apiKey = ApiKeyStore.get(ctx)
            val provider: AIProvider? = if (apiKey.isNullOrBlank()) null else when (providerId) {
                AiProvider.GEMINI -> GeminiAIProvider()
                AiProvider.GROQ -> GroqAIProvider()
            }
            return AICapabilityGate(provider, apiKey)
        }
    }
}
