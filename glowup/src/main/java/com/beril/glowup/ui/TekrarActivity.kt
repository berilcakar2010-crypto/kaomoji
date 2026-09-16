package com.beril.glowup.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.beril.glowup.data.db.GlowUpDatabase
import com.beril.glowup.data.model.TekrarKarti
import com.beril.glowup.databinding.ActivityTekrarBinding
import com.beril.glowup.repetition.SM2
import com.beril.glowup.ui.nav.DpadFocusHelper
import kotlinx.coroutines.launch

/**
 * Aşama 3: SM-2 aralıklı tekrar akışı. Önce gösterim zamanı gelen kartların
 * sayısını gösteren bir merkez ekran (hub) sunar; "Tekrara Başla" ile kartlar
 * tek tek soru → cevap → zorluk puanı akışıyla gözden geçirilir ve her
 * yanıttan sonra bir sonraki gösterim tarihi SM-2 ile yeniden hesaplanır.
 */
class TekrarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTekrarBinding
    private lateinit var db: GlowUpDatabase

    private var bekleyenKartlar: List<TekrarKarti> = emptyList()
    private var mevcutIndeks = 0
    private var cevapGosterildi = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTekrarBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = GlowUpDatabase.getInstance(this)

        binding.yeniKartEkleButonu.setOnClickListener {
            startActivity(Intent(this, KartEkleActivity::class.java))
        }
        binding.tekraraBaslaButonu.setOnClickListener {
            mevcutIndeks = 0
            binding.hubKapsayici.visibility = View.GONE
            binding.incelemeKapsayici.visibility = View.VISIBLE
            kartiGoster()
        }
        binding.cevabiGosterButonu.setOnClickListener {
            cevapGosterildi = true
            binding.cevapMetni.visibility = View.VISIBLE
            binding.cevabiGosterButonu.visibility = View.GONE
            binding.kaliteButonlari.visibility = View.VISIBLE
            DpadFocusHelper.zincirKur(
                listOf(binding.kaliteTekrar, binding.kaliteZor, binding.kaliteIyi, binding.kaliteKolay)
            ) { it.performClick() }
        }

        val kaliteButonlari = listOf(
            binding.kaliteTekrar to 0,
            binding.kaliteZor to 3,
            binding.kaliteIyi to 4,
            binding.kaliteKolay to 5
        )
        kaliteButonlari.forEach { (view, kalite) ->
            view.setOnClickListener { lifecycleScope.launch { kartiDegerlendir(kalite) } }
        }

        lifecycleScope.launch { hubGoster() }
    }

    private suspend fun hubGoster() {
        bekleyenKartlar = db.tekrarKartiDao().gosterimZamaniGelenler(System.currentTimeMillis())
        binding.hubKapsayici.visibility = View.VISIBLE
        binding.incelemeKapsayici.visibility = View.GONE
        binding.tamamlandiMesaji.visibility = View.GONE

        binding.durumMetni.text = if (bekleyenKartlar.isEmpty()) {
            "Gösterim zamanı gelen kart bulunmuyor."
        } else {
            "${bekleyenKartlar.size} kart gösterim için bekliyor."
        }
        binding.tekraraBaslaButonu.visibility = if (bekleyenKartlar.isEmpty()) View.GONE else View.VISIBLE

        DpadFocusHelper.zincirKur(
            listOfNotNull(
                binding.tekraraBaslaButonu.takeIf { bekleyenKartlar.isNotEmpty() },
                binding.yeniKartEkleButonu
            )
        ) { it.performClick() }
    }

    private fun kartiGoster() {
        cevapGosterildi = false
        val kart = bekleyenKartlar[mevcutIndeks]
        binding.ilerlemeMetni.text = "${mevcutIndeks + 1} / ${bekleyenKartlar.size}"
        binding.soruMetni.text = kart.soru
        binding.cevapMetni.text = kart.cevap
        binding.cevapMetni.visibility = View.GONE
        binding.cevabiGosterButonu.visibility = View.VISIBLE
        binding.kaliteButonlari.visibility = View.GONE

        DpadFocusHelper.zincirKur(listOf(binding.cevabiGosterButonu)) { it.performClick() }
    }

    private suspend fun kartiDegerlendir(kalite: Int) {
        val kart = bekleyenKartlar[mevcutIndeks]
        val sonuc = SM2.hesapla(kart, kalite)
        db.tekrarKartiDao().guncelle(
            id = kart.id,
            kf = sonuc.kolaylikFaktoru,
            tekrar = sonuc.tekrarSayisi,
            aralik = sonuc.araligGun,
            sonraki = sonuc.sonrakiGosterimZamani,
            puan = kalite
        )

        mevcutIndeks++
        if (mevcutIndeks < bekleyenKartlar.size) {
            kartiGoster()
        } else {
            val tamamlananSayisi = bekleyenKartlar.size
            hubGoster()
            binding.tamamlandiMesaji.text = "Tekrar oturumu tamamlandı. $tamamlananSayisi kart değerlendirildi."
            binding.tamamlandiMesaji.visibility = View.VISIBLE
        }
    }
}
