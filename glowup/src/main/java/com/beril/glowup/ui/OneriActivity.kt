package com.beril.glowup.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.beril.glowup.data.db.GlowUpDatabase
import com.beril.glowup.data.model.EnerjiSeviyesi
import com.beril.glowup.data.model.Gorev
import com.beril.glowup.data.model.SosyalMod
import com.beril.glowup.databinding.ActivityOneriBinding
import com.beril.glowup.engine.OneriGirdisi
import com.beril.glowup.engine.OneriMotoru
import com.beril.glowup.ui.nav.DpadFocusHelper
import kotlinx.coroutines.launch

/**
 * Aşama 2: "Şimdi ne yapsam" öneri motorunun arayüzü. Kullanıcıdan süre ve
 * enerji/mod girdisi alır, motorun seçtiği görevi gösterir; "Kabul Et" görevi
 * kaydeder, "Başka Öner" aynı görevin ağırlığını azaltıp yeni bir aday getirir.
 *
 * [EXTRA_KATEGORI_ID] verilirse öneri yalnızca o kategoriyle sınırlı kalır;
 * verilmezse motor tüm kategoriler arasından seçim yapar.
 */
class OneriActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOneriBinding
    private lateinit var db: GlowUpDatabase

    private var seciliSureDk: Int? = null
    private var seciliEnerji: EnerjiSeviyesi? = null
    private var seciliSosyalMod: SosyalMod? = null
    private var kategoriId: String? = null
    private var gosterilenOneri: Gorev? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOneriBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = GlowUpDatabase.getInstance(this)

        kategoriId = intent.getStringExtra(EXTRA_KATEGORI_ID)
        if (kategoriId != null) {
            lifecycleScope.launch {
                val kategori = db.kategoriDao().bul(kategoriId!!)
                if (kategori != null) {
                    binding.oneriAltBaslik.text = "Alan: ${kategori.ad}"
                    binding.oneriAltBaslik.visibility = View.VISIBLE
                }
            }
        }

        val sureSecenekleri = listOf(
            binding.sure5 to 5,
            binding.sure15 to 15,
            binding.sure30 to 30,
            binding.sure60 to 60
        )
        sureSecenekleri.forEach { (view, deger) ->
            view.setOnClickListener {
                seciliSureDk = deger
                secimiIsaretle(sureSecenekleri.map { it.first }, view)
            }
        }

        val modSecenekleri = listOf(
            binding.modYuksekSosyal to Pair(EnerjiSeviyesi.YUKSEK, SosyalMod.SOSYAL),
            binding.modYuksekYalniz to Pair(EnerjiSeviyesi.YUKSEK, SosyalMod.YALNIZ),
            binding.modDusukSosyal to Pair(EnerjiSeviyesi.DUSUK, SosyalMod.SOSYAL),
            binding.modDusukYalniz to Pair(EnerjiSeviyesi.DUSUK, SosyalMod.YALNIZ)
        )
        modSecenekleri.forEach { (view, deger) ->
            view.setOnClickListener {
                seciliEnerji = deger.first
                seciliSosyalMod = deger.second
                secimiIsaretle(modSecenekleri.map { it.first }, view)
            }
        }

        binding.oneriButonu.setOnClickListener {
            if (seciliSureDk == null || seciliEnerji == null || seciliSosyalMod == null) {
                Toast.makeText(this, "Devam etmek için süre ve mod seçilmelidir.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch { oneriGetir() }
        }

        binding.kabulEtButonu.setOnClickListener {
            val gorev = gosterilenOneri ?: return@setOnClickListener
            lifecycleScope.launch {
                db.gorevDao().sonYapilmaZamaniGuncelle(gorev.id, System.currentTimeMillis())
                Toast.makeText(
                    this@OneriActivity,
                    "Görev kaydedildi: ${gorev.baslik}",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }

        binding.baskaOnerButonu.setOnClickListener {
            val oncekiOneri = gosterilenOneri
            lifecycleScope.launch {
                if (oncekiOneri != null) {
                    OneriMotoru.begenmedim(db, oncekiOneri)
                }
                oneriGetir(haricTutulacakGorevId = oncekiOneri?.id)
            }
        }

        val ilkZincir = listOf(
            binding.sure5, binding.sure15, binding.sure30, binding.sure60,
            binding.modYuksekSosyal, binding.modYuksekYalniz, binding.modDusukSosyal, binding.modDusukYalniz,
            binding.oneriButonu
        )
        DpadFocusHelper.zincirKur(ilkZincir) { it.performClick() }
    }

    private fun secimiIsaretle(hepsi: List<TextView>, secilen: TextView) {
        hepsi.forEach { it.isActivated = it == secilen }
    }

    private suspend fun oneriGetir(haricTutulacakGorevId: String? = null) {
        val girdi = OneriGirdisi(
            maxSureDk = seciliSureDk!!,
            enerjiSeviyesi = seciliEnerji!!,
            sosyalMod = seciliSosyalMod!!,
            kategoriId = kategoriId
        )
        val gorev = OneriMotoru.oner(db, girdi, haricTutulacakGorevId)
        gosterilenOneri = gorev

        if (gorev == null) {
            binding.sonucKapsayici.visibility = View.GONE
            binding.bosDurumMesaji.visibility = View.VISIBLE
            return
        }

        binding.bosDurumMesaji.visibility = View.GONE
        binding.sonucKapsayici.visibility = View.VISIBLE
        binding.sonucSure.text = "${gorev.sureDk} dk"
        binding.sonucBaslik.text = gorev.baslik
        binding.sonucAciklama.text = gorev.aciklama

        val tamZincir = listOf(
            binding.sure5, binding.sure15, binding.sure30, binding.sure60,
            binding.modYuksekSosyal, binding.modYuksekYalniz, binding.modDusukSosyal, binding.modDusukYalniz,
            binding.oneriButonu, binding.kabulEtButonu, binding.baskaOnerButonu
        )
        DpadFocusHelper.zincirKur(tamZincir) { it.performClick() }
        binding.kabulEtButonu.requestFocus()
    }

    companion object {
        const val EXTRA_KATEGORI_ID = "kategoriId"
    }
}
