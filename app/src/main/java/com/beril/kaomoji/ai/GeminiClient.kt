package com.beril.kaomoji.ai

import android.util.Base64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class GeminiException(message: String) : Exception(message)

/**
 * Google Gemini (https://aistudio.google.com) üzerinden Groq'a alternatif, ücretsiz
 * katmanlı bir sağlayıcı. Aynı üç görevi yapar:
 *  - ses transkripsiyonu (ses dosyası doğrudan modele gönderilir)
 *  - anlatım analizi (sohbet tamamlama)
 *  - otomatik soru/cevap üretimi (JSON çıktı)
 *
 * Model: gemini-2.5-flash — ücretsiz katmanda desteklenen, Google'ın önerdiği
 * güncel Flash modeli (bkz. ai.google.dev/gemini-api/docs/pricing).
 * Anahtar aistudio.google.com/apikey üzerinden ücretsiz alınabiliyor.
 */
object GeminiClient {
    private const val BASE = "https://generativelanguage.googleapis.com/v1beta/models"
    private const val MODEL = "gemini-2.5-flash"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private fun requireKey(apiKey: String?): String {
        if (apiKey.isNullOrBlank()) throw GeminiException("Gemini API anahtarı girilmemiş. Çanta → Kartlar (Anki) veya Anlatımlar ekranından ekleyebilirsin.")
        return apiKey
    }

    // ── 1. Ses transkripsiyonu ────────────────────────────────────────
    fun transcribeAudio(apiKey: String?, audioFile: File): String {
        val key = requireKey(apiKey)
        val b64 = Base64.encodeToString(audioFile.readBytes(), Base64.NO_WRAP)
        val payload = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray()
                    .put(JSONObject().apply {
                        put("text", "Bu ses kaydını Türkçe olarak birebir yazıya dök. Sadece transkript metnini döndür, başka açıklama ekleme.")
                    })
                    .put(JSONObject().apply {
                        put("inline_data", JSONObject().apply {
                            put("mime_type", "audio/mp4")
                            put("data", b64)
                        })
                    })
                )
            }))
        }
        return generateContent(key, payload)
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
        return generateText(key, system, user)
    }

    // ── 3. Otomatik soru/cevap üretimi (flashcard) ──────────────────
    fun generateFlashcards(apiKey: String?, sourceText: String, subjectName: String, n: Int = 8): List<Pair<String, String>> {
        val key = requireKey(apiKey)
        val system = "Sen bir öğretmen asistanısın. Verilen konu metninden aralıklı tekrar (spaced " +
            "repetition) için kısa soru-cevap kartları üretiyorsun. Kurallar: her kart tek bir kavramı " +
            "test etsin, soru net ve tek doğru cevaba sahip olsun, cevap 1-2 cümleyi geçmesin. " +
            "SADECE geçerli bir JSON dizisi döndür, başka hiçbir metin ekleme. Format: " +
            "[{\"q\":\"soru\",\"a\":\"cevap\"}, ...]"
        val user = "Konu alanı: $subjectName\nKaynak metin:\n$sourceText\n\n$n adet kart üret."
        val raw = generateText(key, system, user)
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

    // ── ortak metin üretimi çağrısı (sistem talimatı + kullanıcı mesajı) ──
    private fun generateText(apiKey: String, system: String, user: String): String {
        val payload = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().apply { put("text", system) }))
            })
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().apply { put("text", user) }))
            }))
            put("generationConfig", JSONObject().apply { put("temperature", 0.4) })
        }
        return generateContent(apiKey, payload)
    }

    private fun generateContent(apiKey: String, payload: JSONObject): String {
        val body = payload.toString().toRequestBody("application/json".toMediaType())
        val req = Request.Builder()
            .url("$BASE/$MODEL:generateContent")
            .addHeader("x-goog-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw GeminiException("İstek başarısız (${resp.code}): ${extractError(text)}")
            val json = JSONObject(text)
            val candidates = json.optJSONArray("candidates") ?: throw GeminiException("Beklenmeyen yanıt biçimi")
            if (candidates.length() == 0) throw GeminiException("Boş yanıt (içerik güvenlik filtresine takılmış olabilir)")
            val parts = candidates.getJSONObject(0).getJSONObject("content").getJSONArray("parts")
            return (0 until parts.length())
                .joinToString("") { parts.getJSONObject(it).optString("text", "") }
                .trim()
        }
    }

    private fun extractError(body: String): String = try {
        JSONObject(body).optJSONObject("error")?.optString("message") ?: body.take(200)
    } catch (_: Exception) {
        body.take(200)
    }
}
