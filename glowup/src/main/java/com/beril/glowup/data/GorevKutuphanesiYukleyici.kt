package com.beril.glowup.data

import android.content.Context
import com.beril.glowup.data.model.EnerjiSeviyesi
import com.beril.glowup.data.model.Gorev
import com.beril.glowup.data.model.Kategori
import com.beril.glowup.data.model.SosyalMod
import org.json.JSONObject

/** assets/gorevler.json içindeki görev kütüphanesini okur — kolayca genişletilebilir format. */
object GorevKutuphanesiYukleyici {

    data class Kutuphane(val kategoriler: List<Kategori>, val gorevler: List<Gorev>)

    fun yukle(context: Context, dosyaAdi: String = "gorevler.json"): Kutuphane {
        val metin = context.assets.open(dosyaAdi).bufferedReader().use { it.readText() }
        val kok = JSONObject(metin)

        val kategoriler = mutableListOf<Kategori>()
        val kategorilerJson = kok.getJSONArray("kategoriler")
        for (i in 0 until kategorilerJson.length()) {
            val o = kategorilerJson.getJSONObject(i)
            kategoriler += Kategori(
                id = o.getString("id"),
                ad = o.getString("ad"),
                emoji = o.getString("emoji"),
                renkHex = o.getString("renkHex")
            )
        }

        val gorevler = mutableListOf<Gorev>()
        val gorevlerJson = kok.getJSONArray("gorevler")
        for (i in 0 until gorevlerJson.length()) {
            val o = gorevlerJson.getJSONObject(i)
            gorevler += Gorev(
                id = o.getString("id"),
                kategoriId = o.getString("kategoriId"),
                baslik = o.getString("baslik"),
                aciklama = o.getString("aciklama"),
                sureDk = o.getInt("sureDk"),
                enerjiSeviyesi = EnerjiSeviyesi.valueOf(o.getString("enerjiSeviyesi")),
                sosyalMod = SosyalMod.valueOf(o.getString("sosyalMod"))
            )
        }

        return Kutuphane(kategoriler, gorevler)
    }
}
