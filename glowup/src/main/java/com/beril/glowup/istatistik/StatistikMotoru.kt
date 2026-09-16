package com.beril.glowup.istatistik

import com.beril.glowup.data.db.GlowUpDatabase
import com.beril.glowup.data.model.Kategori
import com.beril.glowup.util.Zaman

/** İstatistik ekranı ve streak/süreklilik takibi için salt-okunur hesaplamalar. */
object StatistikMotoru {

    data class KategoriToplam(val kategori: Kategori, val toplamDakika: Int)
    data class GunToplam(val etiket: String, val toplamDakika: Int)
    data class GunlukOzet(
        val bugunTamamlananGorevSayisi: Int,
        val enUzunSeriKategori: Kategori?,
        val enUzunSeriGunSayisi: Int
    )

    /** Kategori başına toplam çalışma süresi (dakika), en yüksekten en düşüğe. */
    suspend fun kategoriDagilimi(db: GlowUpDatabase): List<KategoriToplam> {
        val kayitlar = db.ilerlemeKaydiDao().hepsi()
        val kategoriler = db.kategoriDao().hepsi().associateBy { it.id }
        return kayitlar.groupBy { it.kategoriId }
            .mapNotNull { (kategoriId, kayitListesi) ->
                val kategori = kategoriler[kategoriId] ?: return@mapNotNull null
                KategoriToplam(kategori, kayitListesi.sumOf { it.toplamSureDk })
            }
            .sortedByDescending { it.toplamDakika }
    }

    /** Son 7 gün için (bugün dahil) toplam çalışma süresi, kronolojik sırayla. */
    suspend fun sonYediGun(db: GlowUpDatabase): List<GunToplam> {
        val kayitlar = db.ilerlemeKaydiDao().hepsi()
        return (6 downTo 0).map { gerideKalanGun ->
            val gunDamgasi = Zaman.gunOnce(gerideKalanGun)
            val toplam = kayitlar.filter { it.gunDamgasi == gunDamgasi }.sumOf { it.toplamSureDk }
            GunToplam(Zaman.kisaGunEtiketi(gunDamgasi), toplam)
        }
    }

    /** Bugünden geriye, kesintisiz tamamlama yapılan gün sayısı (mevcut seri). */
    suspend fun streakGunSayisi(db: GlowUpDatabase, kategoriId: String): Int {
        val aktifGunler = db.ilerlemeKaydiDao().hepsi()
            .filter { it.kategoriId == kategoriId && it.tamamlananGorevSayisi > 0 }
            .map { it.gunDamgasi }
            .toSet()

        var gun = Zaman.gunBaslangici()
        var streak = 0
        while (aktifGunler.contains(gun)) {
            streak++
            gun = Zaman.gunOnce(1, gun)
        }
        return streak
    }

    /** Ana ekranda gösterilen kısa özet: bugün tamamlanan görev sayısı + en uzun seri. */
    suspend fun gunlukOzet(db: GlowUpDatabase): GunlukOzet {
        val bugun = Zaman.gunBaslangici()
        val bugunTamamlanan = db.ilerlemeKaydiDao().hepsi()
            .filter { it.gunDamgasi == bugun }
            .sumOf { it.tamamlananGorevSayisi }

        var enUzunKategori: Kategori? = null
        var enUzunGunSayisi = 0
        db.kategoriDao().hepsi().forEach { kategori ->
            val seri = streakGunSayisi(db, kategori.id)
            if (seri > enUzunGunSayisi) {
                enUzunGunSayisi = seri
                enUzunKategori = kategori
            }
        }

        return GunlukOzet(bugunTamamlanan, enUzunKategori, enUzunGunSayisi)
    }

    /**
     * Hiç çalışılmamış ya da en son çalışılalı en uzun süre geçmiş kategori.
     * Hiç kaydı olmayan bir kategori, en çok ihmal edilmiş sayılır.
     */
    suspend fun enUzunIhmalEdilenAlan(db: GlowUpDatabase): Kategori? {
        val kategoriler = db.kategoriDao().hepsi()
        if (kategoriler.isEmpty()) return null
        val kayitlar = db.ilerlemeKaydiDao().hepsi()

        return kategoriler.minByOrNull { kategori ->
            kayitlar
                .filter { it.kategoriId == kategori.id && it.tamamlananGorevSayisi > 0 }
                .maxOfOrNull { it.gunDamgasi } ?: Long.MIN_VALUE
        }
    }
}
