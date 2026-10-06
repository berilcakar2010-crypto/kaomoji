package com.beril.kaomoji.ai.engine

import java.io.File

/**
 * Sağlayıcıdan bağımsız AI katmanı (§6). Uygulamanın geri kalanı — [AICapabilityGate] dahil —
 * doğrudan Gemini'ye ya da Groq'a değil, bu arayüze konuşur. Yeni bir sağlayıcı eklemek
 * (örn. yerel bir model) yeni bir `AIProvider` implementasyonu yazmak demektir, çağıran
 * taraflardan hiçbirini değiştirmeden. GeminiClient/GroqClient'ın zaten doğru kapsamlı
 * prompt'ları burada yeniden yazılmıyor, sadece tek bir arayüz altında birleştiriliyor.
 */
interface AIProvider {
    suspend fun transcribeAudio(apiKey: String?, audioFile: File): AIResult<String>
    suspend fun analyzeTranscript(apiKey: String?, transcript: String, topic: String?): AIResult<String>
    suspend fun evaluateProgress(apiKey: String?, statsSummary: String): AIResult<String>
    suspend fun generateFlashcards(apiKey: String?, sourceText: String, subjectName: String, n: Int = 8): AIResult<List<Pair<String, String>>>
    suspend fun generateCurriculum(apiKey: String?, docTitle: String, docText: String): AIResult<String>

    // ── Lab 2.0'da yeni, kapasite-bazlı sınırlar için gereken ham çağrılar (§5) ──
    /** Serbest metni iyileştirir — anlamı korur, yapı/dilbilgisi/açıklık önerir. Planı veya
     *  akademik veriyi DEĞİŞTİRMEZ, sadece metin döndürür. */
    suspend fun improveWriting(apiKey: String?, text: String, instruction: String): AIResult<String>

    /** Verilen bağlam özetinden bir çalışma planı ÖNERİSİ üretir. Asla otomatik uygulanmaz —
     *  dönen metin yalnızca bir öneridir, uygulamaya onu yazma yetkisi bu arayüzde yok. */
    suspend fun proposeStudyPlan(apiKey: String?, contextSummary: String, horizonDays: Int): AIResult<String>

    /** Bir kavramı açıklar / sorgular. Kullanıcının kendi akıl yürütmesinin yerine geçmez —
     *  çağıran taraf bunu "önce kendi tahminini yaz" akışının bir adımı olarak kullanmalı. */
    suspend fun explainConcept(apiKey: String?, conceptTitle: String, conceptBody: String?, userAttempt: String?): AIResult<String>

    /** Verilen notları düzenler/özetler/bağlantı önerir. Kaynak, deney veya sonuç ÜRETMEZ —
     *  sadece sağlanan metin üzerinde çalışır. */
    suspend fun organizeResearchNotes(apiKey: String?, rawNotes: String): AIResult<String>
}
