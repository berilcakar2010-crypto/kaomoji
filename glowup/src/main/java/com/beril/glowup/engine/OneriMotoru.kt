package com.beril.glowup.engine

import com.beril.glowup.data.db.GlowUpDatabase
import com.beril.glowup.data.model.EnerjiSeviyesi
import com.beril.glowup.data.model.Gorev
import com.beril.glowup.data.model.SosyalMod
import kotlin.random.Random

/** "Şimdi ne yapsam" motoruna verilen girdi seti. */
data class OneriGirdisi(
    val maxSureDk: Int,
    val enerjiSeviyesi: EnerjiSeviyesi,
    val sosyalMod: SosyalMod,
    /** null ise tüm kategoriler aday havuzuna dahildir. */
    val kategoriId: String? = null
)

/**
 * Süre, enerji/mod ve (varsa) kategori kısıtlarına uyan adaylar arasından bir görev seçer.
 *
 * Seçim kuralı: her adayın skoru `ağırlık × çeşitlilik_çarpanı` olarak hesaplanır.
 * Çeşitlilik çarpanı, görevin en son ne zaman yapıldığına bağlıdır — uzun süredir
 * yapılmamış görevler daha yüksek skor alır. Ağırlık, "başka öner" ile azaltılabilir.
 * Nihai seçim bu skorlarla ağırlıklandırılmış rastgele örnekleme ile yapılır.
 */
object OneriMotoru {

    private const val MAKS_CESITLILIK_GUNU = 30.0

    suspend fun oner(
        db: GlowUpDatabase,
        girdi: OneriGirdisi,
        haricTutulacakGorevId: String? = null
    ): Gorev? {
        val adaylar = db.gorevDao()
            .uygunAdaylar(girdi.maxSureDk, girdi.kategoriId)
            .filter { it.enerjiSeviyesi == girdi.enerjiSeviyesi }
            .filter {
                girdi.sosyalMod == SosyalMod.FARKETMEZ ||
                    it.sosyalMod == SosyalMod.FARKETMEZ ||
                    it.sosyalMod == girdi.sosyalMod
            }
            .filter { it.id != haricTutulacakGorevId }

        if (adaylar.isEmpty()) return null

        val simdi = System.currentTimeMillis()
        val skorlar = adaylar.map { gorev -> gorev to skorHesapla(gorev, simdi) }

        val toplamSkor = skorlar.sumOf { it.second.toDouble() }
        if (toplamSkor <= 0.0) return adaylar.random()

        var esik = Random.nextDouble() * toplamSkor
        for ((gorev, skor) in skorlar) {
            esik -= skor
            if (esik <= 0.0) return gorev
        }
        return skorlar.last().first
    }

    /** "Başka öner": gösterilen görevin ağırlığı azaltılır, tamamen elenmez. */
    suspend fun begenmedim(db: GlowUpDatabase, gorev: Gorev) {
        val yeniAgirlik = (gorev.agirlik * 0.7f).coerceAtLeast(0.15f)
        db.gorevDao().agirlikGuncelle(gorev.id, yeniAgirlik)
    }

    private fun skorHesapla(gorev: Gorev, simdi: Long): Float {
        val gecenGun = gorev.sonYapilmaZamani
            ?.let { (simdi - it) / (1000.0 * 60 * 60 * 24) }
            ?: MAKS_CESITLILIK_GUNU
        val cesitlilikCarpani = 1.0 + gecenGun.coerceIn(0.0, MAKS_CESITLILIK_GUNU) / 10.0
        return (gorev.agirlik * cesitlilikCarpani).toFloat()
    }
}
