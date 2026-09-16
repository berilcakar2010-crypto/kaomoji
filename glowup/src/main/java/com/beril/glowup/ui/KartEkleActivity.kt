package com.beril.glowup.ui

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.beril.glowup.data.db.GlowUpDatabase
import com.beril.glowup.data.model.TekrarKarti
import com.beril.glowup.databinding.ActivityKartEkleBinding
import com.beril.glowup.ui.nav.DpadFocusHelper
import kotlinx.coroutines.launch

/**
 * Yeni bir SM-2 tekrar kartı oluşturur (bilişsel/entelektüel kategoriler).
 * Kart, başlangıç kolaylık faktörüyle (2.5) ve hemen gösterilecek şekilde
 * (sonrakiGosterimZamani = şimdi) kaydedilir.
 */
class KartEkleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKartEkleBinding
    private lateinit var db: GlowUpDatabase
    private var seciliKategoriId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKartEkleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = GlowUpDatabase.getInstance(this)

        val kategoriSecenekleri = listOf(
            binding.kategoriBilissel to "bilissel",
            binding.kategoriEntelektuel to "entelektuel"
        )
        kategoriSecenekleri.forEach { (view, kategoriId) ->
            view.setOnClickListener {
                seciliKategoriId = kategoriId
                kategoriSecenekleri.forEach { (v, _) -> v.isActivated = v == view }
            }
        }

        binding.kaydetButonu.setOnClickListener {
            val soru = binding.soruGirisi.text?.toString()?.trim().orEmpty()
            val cevap = binding.cevapGirisi.text?.toString()?.trim().orEmpty()
            val kategoriId = seciliKategoriId

            if (kategoriId == null || soru.isEmpty() || cevap.isEmpty()) {
                Toast.makeText(
                    this,
                    "Kaydetmek için kategori, soru ve cevap alanlarının tamamı doldurulmalıdır.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                db.tekrarKartiDao().ekle(
                    TekrarKarti(
                        kategoriId = kategoriId,
                        soru = soru,
                        cevap = cevap,
                        sonrakiGosterimZamani = System.currentTimeMillis()
                    )
                )
                Toast.makeText(this@KartEkleActivity, "Kart kaydedildi.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        val zincir: List<android.view.View> = listOf(
            binding.kategoriBilissel, binding.kategoriEntelektuel,
            binding.soruGirisi, binding.cevapGirisi, binding.kaydetButonu
        )
        DpadFocusHelper.zincirKur(zincir) { (it as? TextView)?.performClick() }
    }
}
