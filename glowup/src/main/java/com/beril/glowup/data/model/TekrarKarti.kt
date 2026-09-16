package com.beril.glowup.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** SM-2 aralıklı tekrar kartı — bilişsel/entelektüel kategorilerde kullanılır. */
@Entity(tableName = "tekrar_karti")
data class TekrarKarti(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kategoriId: String,
    val soru: String,
    val cevap: String,
    // SM-2 durumu
    val kolaylikFaktoru: Float = 2.5f,
    val tekrarSayisi: Int = 0,
    val araligGun: Int = 0,
    val sonrakiGosterimZamani: Long = 0L,
    val sonZorlukPuani: Int? = null // 0-5 arası, SM-2 kalite puanı
)
