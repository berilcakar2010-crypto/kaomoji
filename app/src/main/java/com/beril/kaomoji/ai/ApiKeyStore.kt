package com.beril.kaomoji.ai

import android.content.Context
import android.content.SharedPreferences

/** API anahtarları — FocusLock'takinden farklı olarak burada şifrelenmemiş SharedPreferences
 *  kullanılıyor (basitlik için). Cihaz paylaşılıyorsa bunu unutma.
 *  Her sağlayıcının (Groq/Gemini) anahtarı ayrı saklanır, seçili sağlayıcı da burada tutulur. */
object ApiKeyStore {
    private const val PREFS = "kaomoji_ai_prefs"
    private const val KEY_PROVIDER = "ai_provider"
    private const val KEY_GROQ = "groq_api_key"
    private const val KEY_GEMINI = "gemini_api_key"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun prefKeyFor(provider: AiProvider) =
        if (provider == AiProvider.GEMINI) KEY_GEMINI else KEY_GROQ

    fun provider(ctx: Context): AiProvider = AiProvider.fromName(prefs(ctx).getString(KEY_PROVIDER, null))

    fun setProvider(ctx: Context, provider: AiProvider) {
        prefs(ctx).edit().putString(KEY_PROVIDER, provider.name).apply()
    }

    fun get(ctx: Context, provider: AiProvider = provider(ctx)): String? =
        prefs(ctx).getString(prefKeyFor(provider), null)?.takeIf { it.isNotBlank() }

    fun set(ctx: Context, key: String, provider: AiProvider = provider(ctx)) {
        prefs(ctx).edit().putString(prefKeyFor(provider), key.trim()).apply()
    }

    fun clear(ctx: Context, provider: AiProvider = provider(ctx)) {
        prefs(ctx).edit().remove(prefKeyFor(provider)).apply()
    }
}
