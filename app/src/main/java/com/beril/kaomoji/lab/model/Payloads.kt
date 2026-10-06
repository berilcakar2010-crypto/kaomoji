package com.beril.kaomoji.lab.model

import com.beril.kaomoji.lab.learning.LearningDiscipline
import com.beril.kaomoji.lab.learning.LearningStage
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
    val videoFilePath: String? = null,
    val confidence: Int? = null,
    val aiEvaluation: String? = null,
) {
    fun toJson(): String = JSONObject().apply {
        put("language", language)
        audioFilePath?.let { put("audioFilePath", it) }
        videoFilePath?.let { put("videoFilePath", it) }
        confidence?.let { put("confidence", it) }
        aiEvaluation?.let { put("aiEvaluation", it) }
    }.toString()

    companion object {
        fun fromJson(raw: String): ExplanationPayload {
            val j = JSONObject(raw)
            return ExplanationPayload(
                language = j.optString("language"),
                audioFilePath = if (j.has("audioFilePath")) j.optString("audioFilePath") else null,
                videoFilePath = if (j.has("videoFilePath")) j.optString("videoFilePath") else null,
                confidence = if (j.has("confidence")) j.optInt("confidence") else null,
                aiEvaluation = if (j.has("aiEvaluation")) j.optString("aiEvaluation") else null,
            )
        }
    }
}

/** Bir [com.beril.kaomoji.lab.learning.LearningSessionState]'in kalıcı hali. Kullanıcının
 *  denemesi (attemptText) her zaman saklanır — tamamlanmamış bir oturumda bile (§13). */
data class LearningSessionPayload(
    val discipline: LearningDiscipline,
    val stageIndex: Int,
    val attemptText: String,
    val completed: Boolean,
) {
    fun toJson(): String = JSONObject().apply {
        put("discipline", discipline.name)
        put("stageIndex", stageIndex)
        put("attemptText", attemptText)
        put("completed", completed)
    }.toString()

    companion object {
        fun fromJson(raw: String): LearningSessionPayload {
            val j = JSONObject(raw)
            return LearningSessionPayload(
                discipline = LearningDiscipline.valueOf(j.optString("discipline", LearningDiscipline.GENERAL.name)),
                stageIndex = j.optInt("stageIndex", 0),
                attemptText = j.optString("attemptText"),
                completed = j.optBoolean("completed", false),
            )
        }
    }
}

fun LearningSessionPayload.currentStage(): LearningStage =
    com.beril.kaomoji.lab.learning.LearningSessionEngine.stagesFor(discipline)[stageIndex]

/** Proje (§21) — görev listesi değil. Soru/hipotez/sıradaki-eylem bilerek en küçük tutarlı
 *  alt küme; notes serbest metin olarak büyür (deneyler/kararlar/kanıtlar oraya yazılabilir,
 *  her biri için ayrı alan açmak bu aşamada erken optimizasyon olurdu). */
data class ProjectPayload(
    val researchQuestion: String,
    val hypothesis: String? = null,
    val nextAction: String? = null,
    val notes: String = "",
    val status: String = "active",
) {
    fun toJson(): String = JSONObject().apply {
        put("researchQuestion", researchQuestion)
        hypothesis?.let { put("hypothesis", it) }
        nextAction?.let { put("nextAction", it) }
        put("notes", notes)
        put("status", status)
    }.toString()

    companion object {
        fun fromJson(raw: String): ProjectPayload {
            val j = JSONObject(raw)
            return ProjectPayload(
                researchQuestion = j.optString("researchQuestion"),
                hypothesis = if (j.has("hypothesis")) j.optString("hypothesis") else null,
                nextAction = if (j.has("nextAction")) j.optString("nextAction") else null,
                notes = j.optString("notes"),
                status = j.optString("status", "active"),
            )
        }
    }
}

/** Sınav/ödev (§20) — "bu sınav için ne önemli" sorusuna cevap verebilmek için scope +
 *  hazırlık durumu. Tarih zaten Schedule.examDate'te, burada tekrarlanmıyor. */
data class ExamPayload(
    val scope: String,
    val importance: String = "normal",
    val prepStatus: String = "not-started",
) {
    fun toJson(): String = JSONObject().apply {
        put("scope", scope)
        put("importance", importance)
        put("prepStatus", prepStatus)
    }.toString()

    companion object {
        fun fromJson(raw: String): ExamPayload {
            val j = JSONObject(raw)
            return ExamPayload(
                scope = j.optString("scope"),
                importance = j.optString("importance", "normal"),
                prepStatus = j.optString("prepStatus", "not-started"),
            )
        }
    }
}
