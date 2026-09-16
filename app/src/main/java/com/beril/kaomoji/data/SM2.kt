package com.beril.kaomoji.data

import kotlin.math.roundToInt

/**
 * SM-2 aralıklı tekrar algoritması (Anki'nin temel aldığı SuperMemo-2 yöntemi).
 * [kalite] 0-5 arasında bir hatırlama puanıdır: 0-2 hatırlanamadı (tekrar sayacı
 * sıfırlanır, ertesi gün yeniden gösterilir), 3-5 hatırlandı (aralık kolaylık
 * faktörüne göre büyütülerek sonraki gösterim tarihi ertelenir).
 */
object SM2 {

    data class Sonuc(
        val easeFactor: Float,
        val repetitions: Int,
        val intervalDays: Int,
        val nextReviewAt: Long
    )

    fun hesapla(kart: Flashcard, kalite: Int, simdi: Long = System.currentTimeMillis()): Sonuc {
        require(kalite in 0..5) { "Kalite puanı 0-5 aralığında olmalıdır." }

        var yeniEf = kart.easeFactor + (0.1f - (5 - kalite) * (0.08f + (5 - kalite) * 0.02f))
        if (yeniEf < 1.3f) yeniEf = 1.3f

        val yeniTekrar: Int
        val yeniAralik: Int
        if (kalite < 3) {
            yeniTekrar = 0
            yeniAralik = 1
        } else {
            yeniTekrar = kart.repetitions + 1
            yeniAralik = when (yeniTekrar) {
                1 -> 1
                2 -> 6
                else -> (kart.intervalDays * yeniEf).roundToInt().coerceAtLeast(1)
            }
        }

        val gunMs = 24L * 60 * 60 * 1000
        return Sonuc(yeniEf, yeniTekrar, yeniAralik, simdi + yeniAralik.toLong() * gunMs)
    }
}
