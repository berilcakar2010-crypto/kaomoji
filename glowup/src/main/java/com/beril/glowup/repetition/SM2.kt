package com.beril.glowup.repetition

import com.beril.glowup.data.model.TekrarKarti
import kotlin.math.roundToInt

/**
 * SM-2 aralıklı tekrar algoritması (Anki'nin temel aldığı SuperMemo-2 yöntemi).
 * [kalite] 0-5 arasında bir hatırlama puanıdır: 0-2 hatırlanamadı (tekrar sayacı
 * sıfırlanır, ertesi gün yeniden gösterilir), 3-5 hatırlandı (aralık kolaylık
 * faktörüne göre büyütülerek sonraki gösterim tarihi ertelenir).
 */
object SM2 {

    data class Sonuc(
        val kolaylikFaktoru: Float,
        val tekrarSayisi: Int,
        val araligGun: Int,
        val sonrakiGosterimZamani: Long
    )

    fun hesapla(kart: TekrarKarti, kalite: Int, simdi: Long = System.currentTimeMillis()): Sonuc {
        require(kalite in 0..5) { "Kalite puanı 0-5 aralığında olmalıdır." }

        // Kolaylık faktörü, hatırlama başarısız olsa dahi standart SM-2 formülüyle
        // her zaman güncellenir; alt sınır 1.3'tür.
        var yeniKf = kart.kolaylikFaktoru + (0.1f - (5 - kalite) * (0.08f + (5 - kalite) * 0.02f))
        if (yeniKf < 1.3f) yeniKf = 1.3f

        val yeniTekrarSayisi: Int
        val yeniAralik: Int
        if (kalite < 3) {
            yeniTekrarSayisi = 0
            yeniAralik = 1
        } else {
            yeniTekrarSayisi = kart.tekrarSayisi + 1
            yeniAralik = when (yeniTekrarSayisi) {
                1 -> 1
                2 -> 6
                else -> (kart.araligGun * yeniKf).roundToInt().coerceAtLeast(1)
            }
        }

        val sonrakiZaman = simdi + yeniAralik.toLong() * 24L * 60 * 60 * 1000
        return Sonuc(yeniKf, yeniTekrarSayisi, yeniAralik, sonrakiZaman)
    }
}
