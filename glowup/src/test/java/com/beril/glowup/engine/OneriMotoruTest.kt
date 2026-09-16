package com.beril.glowup.engine

import com.beril.glowup.data.model.EnerjiSeviyesi
import com.beril.glowup.data.model.Gorev
import com.beril.glowup.data.model.SosyalMod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OneriMotoruTest {

    private fun gorev(
        id: String = "g1",
        enerji: EnerjiSeviyesi = EnerjiSeviyesi.YUKSEK,
        sosyal: SosyalMod = SosyalMod.YALNIZ,
        agirlik: Float = 1.0f,
        sonYapilmaZamani: Long? = null
    ) = Gorev(
        id = id,
        kategoriId = "bilissel",
        baslik = "Görev",
        aciklama = "Açıklama",
        sureDk = 15,
        enerjiSeviyesi = enerji,
        sosyalMod = sosyal,
        agirlik = agirlik,
        sonYapilmaZamani = sonYapilmaZamani
    )

    // --- uygunMu ---

    @Test
    fun `farkli enerji seviyesi uygun degildir`() {
        val girdi = OneriGirdisi(15, EnerjiSeviyesi.DUSUK, SosyalMod.YALNIZ)
        assertFalse(OneriMotoru.uygunMu(gorev(enerji = EnerjiSeviyesi.YUKSEK), girdi))
    }

    @Test
    fun `sosyal mod farketmez ise gorev veya girdi tarafinda uygundur`() {
        val girdi = OneriGirdisi(15, EnerjiSeviyesi.YUKSEK, SosyalMod.FARKETMEZ)
        assertTrue(OneriMotoru.uygunMu(gorev(sosyal = SosyalMod.SOSYAL), girdi))
        assertTrue(OneriMotoru.uygunMu(gorev(sosyal = SosyalMod.YALNIZ), girdi))
    }

    @Test
    fun `gorev sosyal modu farketmez ise her girdi sosyal moduyla uyar`() {
        val girdi = OneriGirdisi(15, EnerjiSeviyesi.YUKSEK, SosyalMod.SOSYAL)
        assertTrue(OneriMotoru.uygunMu(gorev(sosyal = SosyalMod.FARKETMEZ), girdi))
    }

    @Test
    fun `eslesmeyen sosyal mod uygun degildir`() {
        val girdi = OneriGirdisi(15, EnerjiSeviyesi.YUKSEK, SosyalMod.SOSYAL)
        assertFalse(OneriMotoru.uygunMu(gorev(sosyal = SosyalMod.YALNIZ), girdi))
    }

    // --- skorHesapla ---

    @Test
    fun `hic yapilmamis gorev maksimum cesitlilik carpani alir`() {
        val skor = OneriMotoru.skorHesapla(gorev(agirlik = 1.0f, sonYapilmaZamani = null), simdi = 0L)
        val beklenenCarpan = 1.0 + OneriMotoru.MAKS_CESITLILIK_GUNU / 10.0
        assertEquals(beklenenCarpan.toFloat(), skor, 0.001f)
    }

    @Test
    fun `bugun yapilan gorev carpan 1 alir`() {
        val simdi = 10_000_000L
        val skor = OneriMotoru.skorHesapla(gorev(agirlik = 2.0f, sonYapilmaZamani = simdi), simdi)
        assertEquals(2.0f, skor, 0.001f)
    }

    @Test
    fun `10 gun once yapilan gorev carpan 2 alir`() {
        val gunMs = 24L * 60 * 60 * 1000
        val simdi = 100_000_000L
        val skor = OneriMotoru.skorHesapla(gorev(agirlik = 1.0f, sonYapilmaZamani = simdi - 10 * gunMs), simdi)
        assertEquals(2.0f, skor, 0.001f)
    }

    @Test
    fun `maksimum cesitlilik gununden uzun sureler tavana sabitlenir`() {
        val gunMs = 24L * 60 * 60 * 1000
        val simdi = 1_000_000_000L
        val skorCokEski = OneriMotoru.skorHesapla(gorev(sonYapilmaZamani = simdi - 200 * gunMs), simdi)
        val skorHicYapilmamis = OneriMotoru.skorHesapla(gorev(sonYapilmaZamani = null), simdi)
        assertEquals(skorHicYapilmamis, skorCokEski, 0.001f)
    }

    // --- agirlikliSec ---

    @Test
    fun `bos aday listesi null doner`() {
        assertNull(OneriMotoru.agirlikliSec(emptyList(), 0.5))
    }

    @Test
    fun `dusuk esik degeri ilk yuksek agirlikli adayi secer`() {
        val a = gorev(id = "a")
        val b = gorev(id = "b")
        val secilen = OneriMotoru.agirlikliSec(listOf(a to 1f, b to 3f), rastgeleDeger = 0.1)
        assertEquals("a", secilen?.id)
    }

    @Test
    fun `yuksek esik degeri sonraki agirlikli adayi secer`() {
        val a = gorev(id = "a")
        val b = gorev(id = "b")
        val secilen = OneriMotoru.agirlikliSec(listOf(a to 1f, b to 3f), rastgeleDeger = 0.9)
        assertEquals("b", secilen?.id)
    }

    @Test
    fun `tek aday sifir skorla bile secilir`() {
        val a = gorev(id = "a")
        val secilen = OneriMotoru.agirlikliSec(listOf(a to 0f), rastgeleDeger = 0.5)
        assertEquals("a", secilen?.id)
    }
}
