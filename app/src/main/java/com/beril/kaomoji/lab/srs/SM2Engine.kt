package com.beril.kaomoji.lab.srs

import com.beril.kaomoji.lab.model.FlashcardPayload
import kotlin.math.roundToInt

/**
 * SM-2 aralıklı tekrar — aynı formül `data/SM2.kt`'deki (eski, `Flashcard` veri sınıfına
 * bağlı) sürümle, ama [FlashcardPayload] üzerinde çalışır ve tarih birimi gün (epoch day,
 * `LocalDate.toEpochDay()`), milisaniye değil. Eski SM2.kt'ye kasıtlı olarak dokunulmadı —
 * eski uygulama ona bağımlı, bu Lab 2.0'ın kendi kopyası.
 */
object SM2Engine {
    fun review(payload: FlashcardPayload, quality: Int, todayEpochDay: Long): FlashcardPayload {
        require(quality in 0..5) { "Kalite puanı 0-5 aralığında olmalıdır." }

        var ef = payload.easeFactor + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02))
        if (ef < 1.3) ef = 1.3

        val reps: Int
        val interval: Int
        if (quality < 3) {
            reps = 0
            interval = 1
        } else {
            reps = payload.repetitions + 1
            interval = when (reps) {
                1 -> 1
                2 -> 6
                else -> (payload.intervalDays * ef).roundToInt().coerceAtLeast(1)
            }
        }

        return payload.copy(
            easeFactor = ef,
            repetitions = reps,
            intervalDays = interval,
            nextReviewEpochDay = todayEpochDay + interval,
            lastQuality = quality,
        )
    }
}
