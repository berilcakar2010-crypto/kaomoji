package com.beril.kaomoji.lab.model

import org.json.JSONObject

/**
 * `KnowledgeObjectEntity.payload` tür başına farklı alanlar taşır. Her tür için bu ham JSON'u
 * okuyup/yazan küçük, bağımsız yardımcılar — merkezi bir "payload şeması" sınıfı yok, çünkü
 * yeni bir tür eklemek yeni bir dosyaya yeni bir data class eklemek olmalı, mevcut hiçbir şeyi
 * değiştirmeden (§50 — gelecekte embedding/OCR/el yazısı gibi türler eklenebilsin).
 */

data class MistakePayload(
    val problem: String,
    val attempt: String,
    val whatWentWrong: String,
    val whyItHappened: String,
    val correctReasoning: String,
    val category: String,
    val correctedAttempt: String? = null,
) {
    fun toJson(): String = JSONObject().apply {
        put("problem", problem)
        put("attempt", attempt)
        put("whatWentWrong", whatWentWrong)
        put("whyItHappened", whyItHappened)
        put("correctReasoning", correctReasoning)
        put("category", category)
        correctedAttempt?.let { put("correctedAttempt", it) }
    }.toString()

    companion object {
        fun fromJson(raw: String): MistakePayload {
            val j = JSONObject(raw)
            return MistakePayload(
                problem = j.optString("problem"),
                attempt = j.optString("attempt"),
                whatWentWrong = j.optString("whatWentWrong"),
                whyItHappened = j.optString("whyItHappened"),
                correctReasoning = j.optString("correctReasoning"),
                category = j.optString("category"),
                correctedAttempt = if (j.has("correctedAttempt")) j.optString("correctedAttempt") else null,
            )
        }
    }
}

data class FlashcardPayload(
    val front: String,
    val back: String,
    val easeFactor: Double = 2.5,
    val repetitions: Int = 0,
    val intervalDays: Int = 0,
    val nextReviewEpochDay: Long = 0L,
    val lastQuality: Int? = null,
) {
    fun toJson(): String = JSONObject().apply {
        put("front", front)
        put("back", back)
        put("easeFactor", easeFactor)
        put("repetitions", repetitions)
        put("intervalDays", intervalDays)
        put("nextReviewEpochDay", nextReviewEpochDay)
        lastQuality?.let { put("lastQuality", it) }
    }.toString()

    companion object {
        fun fromJson(raw: String): FlashcardPayload {
            val j = JSONObject(raw)
            return FlashcardPayload(
                front = j.optString("front"),
                back = j.optString("back"),
                easeFactor = j.optDouble("easeFactor", 2.5),
                repetitions = j.optInt("repetitions", 0),
                intervalDays = j.optInt("intervalDays", 0),
                nextReviewEpochDay = j.optLong("nextReviewEpochDay", 0L),
                lastQuality = if (j.has("lastQuality")) j.optInt("lastQuality") else null,
            )
        }
    }
}

data class ExplanationPayload(
    val language: String,
    val audioFilePath: String? = null,
    val confidence: Int? = null,
    val aiEvaluation: String? = null,
) {
    fun toJson(): String = JSONObject().apply {
        put("language", language)
        audioFilePath?.let { put("audioFilePath", it) }
        confidence?.let { put("confidence", it) }
        aiEvaluation?.let { put("aiEvaluation", it) }
    }.toString()

    companion object {
        fun fromJson(raw: String): ExplanationPayload {
            val j = JSONObject(raw)
            return ExplanationPayload(
                language = j.optString("language"),
                audioFilePath = if (j.has("audioFilePath")) j.optString("audioFilePath") else null,
                confidence = if (j.has("confidence")) j.optInt("confidence") else null,
                aiEvaluation = if (j.has("aiEvaluation")) j.optString("aiEvaluation") else null,
            )
        }
    }
}
