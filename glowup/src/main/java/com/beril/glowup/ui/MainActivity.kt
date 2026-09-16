package com.beril.glowup.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.beril.glowup.data.GorevKutuphanesiYukleyici
import com.beril.glowup.data.db.GlowUpDatabase
import com.beril.glowup.databinding.ActivityMainBinding
import com.beril.glowup.databinding.ItemKategoriKartiBinding
import com.beril.glowup.ui.nav.DpadFocusHelper
import kotlinx.coroutines.launch

/**
 * Aşama 1 iskeleti: görev kütüphanesini JSON'dan okuyup Room'a yazar,
 * kategorileri D-pad ile gezilebilir bir dikey liste olarak gösterir.
 * "Şimdi ne yapsam" öneri motoru Aşama 2'de bu ekrana bağlanacak.
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

            val kategoriler = db.kategoriDao().hepsi()
            kategoriler.forEach { kategori ->
                val itemBinding = ItemKategoriKartiBinding.inflate(
                    LayoutInflater.from(this@MainActivity), binding.kategoriListesi, false
                )
                itemBinding.kategoriEmoji.text = kategori.emoji
                itemBinding.kategoriAd.text = kategori.ad
                itemBinding.root.id = VIEW_ID_BASE + kategoriler.indexOf(kategori)
                itemBinding.root.setOnClickListener {
                    Toast.makeText(
                        this@MainActivity,
                        "${kategori.ad}: öneri motoru Aşama 2'de gelecek",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                binding.kategoriListesi.addView(itemBinding.root)
            }

            DpadFocusHelper.dikeyZincirKur(binding.kategoriListesi) { secilenView ->
                secilenView.performClick()
            }
        }
    }

    companion object {
        private const val VIEW_ID_BASE = 1000
    }
}
