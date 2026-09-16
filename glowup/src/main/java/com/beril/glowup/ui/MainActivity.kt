package com.beril.glowup.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.beril.glowup.data.GorevKutuphanesiYukleyici
import com.beril.glowup.data.db.GlowUpDatabase
import com.beril.glowup.databinding.ActivityMainBinding
import com.beril.glowup.databinding.ItemKategoriKartiBinding
import com.beril.glowup.ui.nav.DpadFocusHelper
import kotlinx.coroutines.launch

/**
 * Görev kütüphanesini JSON'dan okuyup Room'a yazar; genel "Şimdi Ne Yapsam"
 * girişini ve kategorileri D-pad ile gezilebilir bir dikey liste olarak gösterir.
 * Her girişin seçilmesi, öneri motorunun çalıştığı [OneriActivity]'yi açar.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = GlowUpDatabase.getInstance(this)

        lifecycleScope.launch {
            val kutuphane = GorevKutuphanesiYukleyici.yukle(this@MainActivity)
            db.kategoriDao().hepsiniEkle(kutuphane.kategoriler)
            db.gorevDao().hepsiniEkle(kutuphane.gorevler)

            val genelGirisBinding = ItemKategoriKartiBinding.inflate(
                LayoutInflater.from(this@MainActivity), binding.kategoriListesi, false
            )
            genelGirisBinding.kategoriEmoji.text = "🔎"
            genelGirisBinding.kategoriAd.text = "Şimdi Ne Yapsam"
            genelGirisBinding.root.id = VIEW_ID_BASE
            genelGirisBinding.root.setOnClickListener {
                startActivity(Intent(this@MainActivity, OneriActivity::class.java))
            }
            binding.kategoriListesi.addView(genelGirisBinding.root)

            val kategoriler = db.kategoriDao().hepsi()
            kategoriler.forEachIndexed { index, kategori ->
                val itemBinding = ItemKategoriKartiBinding.inflate(
                    LayoutInflater.from(this@MainActivity), binding.kategoriListesi, false
                )
                itemBinding.kategoriEmoji.text = kategori.emoji
                itemBinding.kategoriAd.text = kategori.ad
                itemBinding.root.id = VIEW_ID_BASE + 1 + index
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
            tekrarGirisBinding.root.id = VIEW_ID_BASE + 1 + kategoriler.size
            tekrarGirisBinding.root.setOnClickListener {
                startActivity(Intent(this@MainActivity, TekrarActivity::class.java))
            }
            binding.kategoriListesi.addView(tekrarGirisBinding.root)

            val istatistikGirisBinding = ItemKategoriKartiBinding.inflate(
                LayoutInflater.from(this@MainActivity), binding.kategoriListesi, false
            )
            istatistikGirisBinding.kategoriEmoji.text = "📊"
            istatistikGirisBinding.kategoriAd.text = "İstatistikler"
            istatistikGirisBinding.root.id = VIEW_ID_BASE + 2 + kategoriler.size
            istatistikGirisBinding.root.setOnClickListener {
                startActivity(Intent(this@MainActivity, StatistikActivity::class.java))
            }
            binding.kategoriListesi.addView(istatistikGirisBinding.root)

            DpadFocusHelper.dikeyZincirKur(binding.kategoriListesi) { secilenView ->
                secilenView.performClick()
            }
        }
    }

    companion object {
        private const val VIEW_ID_BASE = 1000
    }
}
