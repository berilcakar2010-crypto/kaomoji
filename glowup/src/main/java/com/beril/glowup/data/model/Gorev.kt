package com.beril.glowup.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EnerjiSeviyesi { YUKSEK, DUSUK }
enum class SosyalMod { SOSYAL, YALNIZ, FARKETMEZ }

/** Görev kütüphanesindeki tek bir görev tanımı (JSON'dan Room'a aktarılır). */
@Entity(tableName = "gorev")
data class Gorev(
    @PrimaryKey val id: String,
    val kategoriId: String,
    val baslik: String,
    val aciklama: String,
    val sureDk: Int,
    val enerjiSeviyesi: EnerjiSeviyesi,
    val sosyalMod: SosyalMod,
    val agirlik: Float = 1.0f,
    val sonYapilmaZamani: Long? = null
)
