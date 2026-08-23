package com.beril.kaomoji.ai

import android.content.Context
import android.content.SharedPreferences
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/** API anahtarı — FocusLock'takinden farklı olarak burada şifrelenmemiş SharedPreferences
 *  kullanılıyor (basitlik için). Cihaz paylaşılıyorsa bunu unutma. */
object ApiKeyStore {
    private const val PREFS = "kaomoji_ai_prefs"
    private const val KEY = "groq_api_key"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun get(ctx: Context): String? = prefs(ctx).getString(KEY, null)?.takeIf { it.isNotBlank() }
    fun set(ctx: Context, key: String) {
        prefs(ctx).edit().putString(KEY, key.trim()).apply()
    }
    fun clear(ctx: Context) {
        prefs(ctx).edit().remove(KEY).apply()
    }
}

class GroqException(message: String) : Exception(message)

/**
 * Groq (https://console.groq.com) üzerinden:
 *  - ses transkripsiyonu (Whisper Large v3)
 *  - anlatım analizi (Llama 3.3 70B, sohbet tamamlama)
 *  - otomatik soru/cevap üretimi (aynı model, JSON çıktı)
 *
 * Not: Bu istekler internet gerektirir ve Beril'in kendi Groq API anahtarını
 * kullanır (console.groq.com üzerinden ücretsiz alınabiliyor — FocusLock'ta
 * kullandığın anahtarla aynısını burada da kullanabilirsin).
 */
object GroqClient {
    private const val BASE = "https://api.groq.com/openai/v1"
    private const val CHAT_MODEL = "llama-3.3-70b-versatile"
    private const val WHISPER_MODEL = "whisper-large-v3"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private fun requireKey(apiKey: String?): String {
        if (apiKey.isNullOrBlank()) throw GroqException("Groq API anahtarı girilmemiş. Çanta → Kartlar (Anki) veya Anlatımlar ekranından ekleyebilirsin.")
        return apiKey
    }

    // ── 1. Ses transkripsiyonu ────────────────────────────────────────
    fun transcribeAudio(apiKey: String?, audioFile: File): String {
        val key = requireKey(apiKey)
        val mediaType = "audio/m4a".toMediaType()
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("model", WHISPER_MODEL)
            .addFormDataPart("language", "tr")
            .addFormDataPart(
                "file", audioFile.name,
                audioFile.asRequestBody(mediaType)
            )
            .build()

        val req = Request.Builder()
            .url("$BASE/audio/transcriptions")
            .addHeader("Authorization", "Bearer $key")
            .post(body)
            .build()

        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw GroqException("Transkripsiyon başarısız (${resp.code}): ${extractError(text)}")
            return JSONObject(text).optString("text", "").trim()
        }
    }

    // ── 2. Anlatım analizi ──────────────────────────────────────────
    fun analyzeTranscript(apiKey: String?, transcript: String, topic: String?): String {
        val key = requireKey(apiKey)
        val system = "Sen bir öğrenme koçusun. Öğrenci bir konuyu kitaba bakmadan sesli anlattı ve bu " +
            "onun transkripsiyonu. Görevin: (1) doğru anlattığı noktaları kısaca onayla, " +
            "(2) eksik/belirsiz kalan kavramları listele, (3) bir sonraki adım için tek somut öneri ver. " +
            "Türkçe, kısa (max 120 kelime), yargılamadan, doğrudan cevap ver."
        val user = buildString {
            if (!topic.isNullOrBlank()) append("Konu: $topic\n\n")
            append("Anlatım: $transcript")
        }
        return chatCompletion(key, system, user)
    }

    // ── 3. Otomatik soru/cevap üretimi (flashcard) ──────────────────
    /** Kaynak metinden (birim görevleri veya bir anlatım) N adet soru-cevap çifti üretir. */
    fun generateFlashcards(apiKey: String?, sourceText: String, subjectName: String, n: Int = 8): List<Pair<String, String>> {
        val key = requireKey(apiKey)
        val system = "Sen bir öğretmen asistanısın. Verilen konu metninden aralıklı tekrar (spaced " +
            "repetition) için kısa soru-cevap kartları üretiyorsun. Kurallar: her kart tek bir kavramı " +
            "test etsin, soru net ve tek doğru cevaba sahip olsun, cevap 1-2 cümleyi geçmesin. " +
            "SADECE geçerli bir JSON dizisi döndür, başka hiçbir metin ekleme. Format: " +
            "[{\"q\":\"soru\",\"a\":\"cevap\"}, ...]"
        val user = "Konu alanı: $subjectName\nKaynak metin:\n$sourceText\n\n$n adet kart üret."
        val raw = chatCompletion(key, system, user)
        return parseFlashcardJson(raw)
    }

    private fun parseFlashcardJson(raw: String): List<Pair<String, String>> {
        // model bazen ```json ... ``` ile sarmalıyor, temizle
        val cleaned = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val start = cleaned.indexOf('[')
        val end = cleaned.lastIndexOf(']')
        if (start == -1 || end == -1 || end < start) return emptyList()
        val jsonSlice = cleaned.substring(start, end + 1)
        return try {
            val arr = JSONArray(jsonSlice)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val q = o.optString("q", "").trim()
                val a = o.optString("a", "").trim()
                if (q.isNotBlank() && a.isNotBlank()) q to a else null
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ── ortak sohbet tamamlama çağrısı ──────────────────────────────
    private fun chatCompletion(apiKey: String, system: String, user: String): String {
        val payload = JSONObject().apply {
            put("model", CHAT_MODEL)
            put("temperature", 0.4)
            put("messages", JSONArray().apply {
                put(JSONObject().apply { put("role", "system"); put("content", system) })
                put(JSONObject().apply { put("role", "user"); put("content", user) })
            })
        }
        val body = payload.toString().toRequestBody("application/json".toMediaType())
        val req = Request.Builder()
            .url("$BASE/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw GroqException("İstek başarısız (${resp.code}): ${extractError(text)}")
            val json = JSONObject(text)
            val choices = json.optJSONArray("choices") ?: throw GroqException("Beklenmeyen yanıt biçimi")
            if (choices.length() == 0) throw GroqException("Boş yanıt")
            return choices.getJSONObject(0).getJSONObject("message").getString("content").trim()
        }
    }

    private fun extractError(body: String): String = try {
        JSONObject(body).optJSONObject("error")?.optString("message") ?: body.take(200)
    } catch (_: Exception) {
        body.take(200)
    }
}
