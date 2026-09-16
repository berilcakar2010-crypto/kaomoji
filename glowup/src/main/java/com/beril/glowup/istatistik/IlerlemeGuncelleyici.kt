package com.beril.glowup.istatistik

import com.beril.glowup.data.db.GlowUpDatabase
import com.beril.glowup.data.model.IlerlemeKaydi
import com.beril.glowup.util.Zaman

/** Bir görev tamamlandığında ilgili kategorinin günlük özet kaydını günceller (upsert). */
object IlerlemeGuncelleyici {

    suspend fun gorevTamamlandi(db: GlowUpDatabase, kategoriId: String, sureDk: Int) {
        val gun = Zaman.gunBaslangici()
        val mevcut = db.ilerlemeKaydiDao().bul(kategoriId, gun)
        db.ilerlemeKaydiDao().kaydet(
            IlerlemeKaydi(
                kategoriId = kategoriId,
                gunDamgasi = gun,
                toplamSureDk = (mevcut?.toplamSureDk ?: 0) + sureDk,
                tamamlananGorevSayisi = (mevcut?.tamamlananGorevSayisi ?: 0) + 1
            )
        )
    }
}
