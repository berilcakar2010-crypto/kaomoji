package com.beril.glowup.util

import java.util.Calendar

/** Gün bazlı hesaplamalar için yardımcı fonksiyonlar (streak, günlük özet, vb.). */
object Zaman {

    private const val GUN_MS = 24L * 60 * 60 * 1000

    /** Verilen zamanın (varsayılan: şimdi) yerel takvim gününün başlangıcı (00:00:00.000). */
    fun gunBaslangici(zamanMillis: Long = System.currentTimeMillis()): Long {
        val takvim = Calendar.getInstance()
        takvim.timeInMillis = zamanMillis
        takvim.set(Calendar.HOUR_OF_DAY, 0)
        takvim.set(Calendar.MINUTE, 0)
        takvim.set(Calendar.SECOND, 0)
        takvim.set(Calendar.MILLISECOND, 0)
        return takvim.timeInMillis
    }

    fun gunOnce(gunSayisi: Int, referans: Long = gunBaslangici()): Long = referans - gunSayisi * GUN_MS

    /** Pzt/Sal/... biçiminde kısa gün etiketi. */
    fun kisaGunEtiketi(zamanMillis: Long): String {
        val takvim = Calendar.getInstance()
        takvim.timeInMillis = zamanMillis
        val gunler = arrayOf("Paz", "Pzt", "Sal", "Çar", "Per", "Cum", "Cmt")
        return gunler[takvim.get(Calendar.DAY_OF_WEEK) - 1]
    }
}
