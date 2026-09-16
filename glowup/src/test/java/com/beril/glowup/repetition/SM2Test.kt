package com.beril.glowup.repetition

import com.beril.glowup.data.model.TekrarKarti
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SM2Test {

    private val gunMs = 24L * 60 * 60 * 1000

    private fun kart(kf: Float = 2.5f, tekrar: Int = 0, aralik: Int = 0) =
        TekrarKarti(kategoriId = "bilissel", soru = "S", cevap = "C", kolaylikFaktoru = kf, tekrarSayisi = tekrar, araligGun = aralik)

    @Test
    fun `ilk basarili tekrar araligi 1 gundur`() {
        val sonuc = SM2.hesapla(kart(), kalite = 5, simdi = 1000L)
        assertEquals(1, sonuc.tekrarSayisi)
        assertEquals(1, sonuc.araligGun)
        assertEquals(2.6f, sonuc.kolaylikFaktoru, 0.001f)
        assertEquals(1000L + gunMs, sonuc.sonrakiGosterimZamani)
    }

    @Test
    fun `ikinci basarili tekrar araligi 6 gundur`() {
        val sonuc = SM2.hesapla(kart(kf = 2.6f, tekrar = 1, aralik = 1), kalite = 5, simdi = 0L)
        assertEquals(2, sonuc.tekrarSayisi)
        assertEquals(6, sonuc.araligGun)
    }

    @Test
    fun `ucuncu ve sonraki tekrarlar onceki aralik kere kolaylik faktoru ile hesaplanir`() {
        val sonuc = SM2.hesapla(kart(kf = 2.5f, tekrar = 2, aralik = 6), kalite = 4, simdi = 0L)
        // kalite=4 icin SM-2 formulunde kolaylik faktoru degismez (delta = 0)
        assertEquals(2.5f, sonuc.kolaylikFaktoru, 0.001f)
        assertEquals(3, sonuc.tekrarSayisi)
        assertEquals(15, sonuc.araligGun) // round(6 * 2.5)
    }

    @Test
    fun `kalite 5 kolaylik faktorunu 0-1 artirir`() {
        val sonuc = SM2.hesapla(kart(kf = 2.5f), kalite = 5, simdi = 0L)
        assertEquals(2.6f, sonuc.kolaylikFaktoru, 0.001f)
    }

    @Test
    fun `kalite 3 kolaylik faktorunu 0-14 azaltir`() {
        val sonuc = SM2.hesapla(kart(kf = 2.5f), kalite = 3, simdi = 0L)
        assertEquals(2.36f, sonuc.kolaylikFaktoru, 0.001f)
    }

    @Test
    fun `basarisiz hatirlama tekrar sayacini sifirlar ve araligi 1 gune indirir`() {
        val sonuc = SM2.hesapla(kart(kf = 2.5f, tekrar = 5, aralik = 30), kalite = 1, simdi = 5000L)
        assertEquals(0, sonuc.tekrarSayisi)
        assertEquals(1, sonuc.araligGun)
        assertEquals(5000L + gunMs, sonuc.sonrakiGosterimZamani)
    }

    @Test
    fun `kolaylik faktoru 1-3 tabaninin altina inmez`() {
        val sonuc = SM2.hesapla(kart(kf = 1.3f, tekrar = 2, aralik = 6), kalite = 0, simdi = 0L)
        assertEquals(1.3f, sonuc.kolaylikFaktoru, 0.001f)
    }

    @Test
    fun `gecersiz kalite puani hata firlatir`() {
        assertThrows(IllegalArgumentException::class.java) {
            SM2.hesapla(kart(), kalite = 6)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SM2.hesapla(kart(), kalite = -1)
        }
    }
}
