package com.beril.glowup.ui

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.beril.glowup.R
import com.beril.glowup.data.db.GlowUpDatabase
import com.beril.glowup.databinding.ActivityIstatistikBinding
import com.beril.glowup.istatistik.StatistikMotoru
import kotlinx.coroutines.launch

/**
 * Aşama 3 istatistik ekranı: kategori dağılımı, son 7 günün trendi, kategori
 * bazlı seri (streak) ve en uzun süredir ihmal edilen alan. Düşük özellikli
 * cihazlarda okunaklı kalması için grafikler basit, tek renkli yatay çubuklarla
 * çizilir — kütüphane bağımlılığı veya animasyon yoktur.
 */
class StatistikActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIstatistikBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIstatistikBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = GlowUpDatabase.getInstance(this)
        lifecycleScope.launch {
            val kategoriDagilimi = StatistikMotoru.kategoriDagilimi(db)
            val sonYediGun = StatistikMotoru.sonYediGun(db)
            val kategoriler = db.kategoriDao().hepsi()

            if (kategoriDagilimi.isEmpty() && sonYediGun.all { it.toplamDakika == 0 }) {
                binding.bosDurumMesaji.visibility = View.VISIBLE
            }

            val maksKategoriDakika = kategoriDagilimi.maxOfOrNull { it.toplamDakika } ?: 0
            kategoriDagilimi.forEach { toplam ->
                barSatiriEkle(
                    binding.kategoriDagilimContainer,
                    "${toplam.kategori.emoji} ${toplam.kategori.ad}",
                    toplam.toplamDakika,
                    maksKategoriDakika
                )
            }

            val maksGunDakika = sonYediGun.maxOfOrNull { it.toplamDakika } ?: 0
            sonYediGun.forEach { gun ->
                barSatiriEkle(binding.haftalikTrendContainer, gun.etiket, gun.toplamDakika, maksGunDakika)
            }

            val maksStreak = kategoriler.maxOfOrNull { StatistikMotoru.streakGunSayisi(db, it.id) } ?: 0
            kategoriler.forEach { kategori ->
                val streak = StatistikMotoru.streakGunSayisi(db, kategori.id)
                streakSatiriEkle(binding.streakContainer, "${kategori.emoji} ${kategori.ad}", streak, maksStreak)
            }

            val ihmalEdilen = StatistikMotoru.enUzunIhmalEdilenAlan(db)
            binding.ihmalMesaji.text = if (ihmalEdilen != null) {
                "En uzun süredir çalışılmayan alan: ${ihmalEdilen.emoji} ${ihmalEdilen.ad}"
            } else {
                ""
            }
        }
    }

    private fun barSatiriEkle(container: LinearLayout, etiket: String, deger: Int, maksDeger: Int) {
        val satir = satirIskeletiOlustur()

        val etiketView = etiketGorunumuOlustur(etiket)
        satir.addView(etiketView)

        val barGenislikMaks = dp(140)
        val barGenislik = when {
            maksDeger <= 0 -> 0
            deger <= 0 -> 0
            else -> (barGenislikMaks * deger / maksDeger).coerceAtLeast(dp(4))
        }
        val bar = View(this)
        bar.setBackgroundColor(ContextCompat.getColor(this, R.color.tie_burgundy))
        val barParams = LinearLayout.LayoutParams(barGenislik, dp(14))
        barParams.marginEnd = dp(8)
        bar.layoutParams = barParams
        satir.addView(bar)

        satir.addView(degerGorunumuOlustur("$deger dk"))
        container.addView(satir)
    }

    private fun streakSatiriEkle(container: LinearLayout, etiket: String, gunSayisi: Int, maksGunSayisi: Int) {
        val satir = satirIskeletiOlustur()
        satir.addView(etiketGorunumuOlustur(etiket))

        val barGenislikMaks = dp(140)
        val barGenislik = when {
            maksGunSayisi <= 0 -> 0
            gunSayisi <= 0 -> 0
            else -> (barGenislikMaks * gunSayisi / maksGunSayisi).coerceAtLeast(dp(4))
        }
        val bar = View(this)
        bar.setBackgroundColor(ContextCompat.getColor(this, R.color.eye_lavender))
        val barParams = LinearLayout.LayoutParams(barGenislik, dp(14))
        barParams.marginEnd = dp(8)
        bar.layoutParams = barParams
        satir.addView(bar)

        satir.addView(degerGorunumuOlustur("$gunSayisi gün"))
        container.addView(satir)
    }

    private fun satirIskeletiOlustur(): LinearLayout {
        val satir = LinearLayout(this)
        satir.orientation = LinearLayout.HORIZONTAL
        satir.gravity = Gravity.CENTER_VERTICAL
        satir.setPadding(0, dp(4), 0, dp(4))
        return satir
    }

    private fun etiketGorunumuOlustur(metin: String): TextView {
        val view = TextView(this)
        view.text = metin
        view.setTextColor(ContextCompat.getColor(this, R.color.ink_dark))
        view.layoutParams = LinearLayout.LayoutParams(dp(96), LinearLayout.LayoutParams.WRAP_CONTENT)
        return view
    }

    private fun degerGorunumuOlustur(metin: String): TextView {
        val view = TextView(this)
        view.text = metin
        view.textSize = 12f
        view.setTextColor(ContextCompat.getColor(this, R.color.ink_muted))
        return view
    }

    private fun dp(deger: Int): Int = (deger * resources.displayMetrics.density).toInt()
}
