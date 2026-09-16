package com.beril.glowup.data.model

import androidx.room.Entity

/** Günlük/kategori bazlı özet — streak ve istatistik ekranının kaynağı. */
@Entity(tableName = "ilerleme_kaydi", primaryKeys = ["kategoriId", "gunDamgasi"])
data class IlerlemeKaydi(
    val kategoriId: String,
    /** Günün başlangıcına yuvarlanmış epoch millis — günlük tekillik anahtarı. */
    val gunDamgasi: Long,
    val toplamSureDk: Int,
    val tamamlananGorevSayisi: Int
)
