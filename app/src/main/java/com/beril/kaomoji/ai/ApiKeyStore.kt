package com.beril.kaomoji.ai

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** API anahtarları Android Keystore destekli [EncryptedSharedPreferences] ile saklanır (§35
 *  — "secure API-key storage"). Her sağlayıcının (Groq/Gemini) anahtarı ayrı saklanır, seçili
 *  sağlayıcı da burada tutulur. Şifreleme anahtarı cihazın donanım destekli keystore'unda
 *  kalır, uygulama verisiyle birlikte dışa aktarılmaz. */
object ApiKeyStore {
    private const val PREFS = "kaomoji_ai_prefs_v2"
    private const val KEY_PROVIDER = "ai_provider"
    private const val KEY_GROQ = "groq_api_key"
    private const val KEY_GEMINI = "gemini_api_key"

    @Volatile private var cached: SharedPreferences? = null

    private fun prefs(ctx: Context): SharedPreferences = cached ?: synchronized(this) {
        cached ?: run {
            val masterKey = MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            EncryptedSharedPreferences.create(
                ctx,
                PREFS,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            ).also { cached = it }
        }
    }

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
