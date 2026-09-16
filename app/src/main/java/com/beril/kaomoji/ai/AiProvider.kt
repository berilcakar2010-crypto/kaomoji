package com.beril.kaomoji.ai

/** Desteklenen yapay zeka sağlayıcıları. Kullanıcı ikisinden birini seçip kendi ücretsiz
 *  API anahtarını girer; anahtarlar sağlayıcı bazında ayrı saklanır (bkz. ApiKeyStore). */
enum class AiProvider(val label: String, val keyHint: String, val keySource: String) {
    GROQ("Groq (Llama)", "gsk_...", "console.groq.com üzerinden ücretsiz"),
    GEMINI("Gemini (Google)", "AIza...", "aistudio.google.com/apikey üzerinden ücretsiz");

    companion object {
        fun fromName(name: String?): AiProvider =
            entries.firstOrNull { it.name == name } ?: GROQ
    }
}
