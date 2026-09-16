package com.beril.glowup.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.beril.glowup.data.GorevKutuphanesiYukleyici
import com.beril.glowup.data.db.GlowUpDatabase
import com.beril.glowup.databinding.ActivityMainBinding
import com.beril.glowup.databinding.ItemKategoriKartiBinding
import com.beril.glowup.istatistik.StatistikMotoru
import com.beril.glowup.ui.nav.DpadFocusHelper
import kotlinx.coroutines.launch

/**
 * Ana ekran: tek tuşla tetiklenen büyük "Şimdi Ne Yapsam" girişi + kısa günlük
 * istatistik özeti, altında kategoriler ve Tekrar Kartları / İstatistikler
 * girişlerinin bulunduğu D-pad ile gezilebilir dikey liste. Görev kütüphanesini
 * JSON'dan okuyup Room'a yazma işlemi de burada yapılır.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = GlowUpDatabase.getInstance(this)

        binding.buyukOneriButonu.setOnClickListener {
            startActivity(Intent(this, OneriActivity::class.java))
        }

        lifecycleScope.launch {
            val kutuphane = GorevKutuphanesiYukleyici.yukle(this@MainActivity)
            db.kategoriDao().hepsiniEkle(kutuphane.kategoriler)
            db.gorevDao().hepsiniEkle(kutuphane.gorevler)

            val ozet = StatistikMotoru.gunlukOzet(db)
            binding.gunlukOzetMetni.text = buildString {
                append("Bugün: ${ozet.bugunTamamlananGorevSayisi} görev")
                if (ozet.enUzunSeriKategori != null && ozet.enUzunSeriGunSayisi > 0) {
                    append(" · En uzun seri: ${ozet.enUzunSeriGunSayisi} gün (${ozet.enUzunSeriKategori.ad})")
                }
            }

            val kategoriler = db.kategoriDao().hepsi()
            kategoriler.forEachIndexed { index, kategori ->
                val itemBinding = ItemKategoriKartiBinding.inflate(
                    LayoutInflater.from(this@MainActivity), binding.kategoriListesi, false
                )
                itemBinding.kategoriEmoji.text = kategori.emoji
                itemBinding.kategoriAd.text = kategori.ad
                itemBinding.root.id = VIEW_ID_BASE + index
                itemBinding.root.setOnClickListener {
                    startActivity(
                        Intent(this@MainActivity, OneriActivity::class.java)
                            .putExtra(OneriActivity.EXTRA_KATEGORI_ID, kategori.id)
                    )
                }
                binding.kategoriListesi.addView(itemBinding.root)
            }

            val tekrarGirisBinding = ItemKategoriKartiBinding.inflate(
                LayoutInflater.from(this@MainActivity), binding.kategoriListesi, false
            )
            tekrarGirisBinding.kategoriEmoji.text = "🔁"
            tekrarGirisBinding.kategoriAd.text = "Tekrar Kartları"
            tekrarGirisBinding.root.id = VIEW_ID_BASE + kategoriler.size
            tekrarGirisBinding.root.setOnClickListener {
                startActivity(Intent(this@MainActivity, TekrarActivity::class.java))
            }
            binding.kategoriListesi.addView(tekrarGirisBinding.root)

            val istatistikGirisBinding = ItemKategoriKartiBinding.inflate(
                LayoutInflater.from(this@MainActivity), binding.kategoriListesi, false
            )
            istatistikGirisBinding.kategoriEmoji.text = "📊"
            istatistikGirisBinding.kategoriAd.text = "İstatistikler"
            istatistikGirisBinding.root.id = VIEW_ID_BASE + 1 + kategoriler.size
            istatistikGirisBinding.root.setOnClickListener {
                startActivity(Intent(this@MainActivity, StatistikActivity::class.java))
            }
            binding.kategoriListesi.addView(istatistikGirisBinding.root)

            // Büyük giriş + kategori listesi tek bir D-pad zincirinde, büyük giriş her zaman ilk odakta.
            val listeElemanlari = (0 until binding.kategoriListesi.childCount)
                .map { binding.kategoriListesi.getChildAt(it) }
            val tamZincir: List<View> = listOf(binding.buyukOneriButonu) + listeElemanlari
            DpadFocusHelper.zincirKur(tamZincir) { it.performClick() }
        }
    }

    companion object {
        private const val VIEW_ID_BASE = 1000
    }
}
