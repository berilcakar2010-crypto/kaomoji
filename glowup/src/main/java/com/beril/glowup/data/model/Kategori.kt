package com.beril.glowup.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Beş görev alanı: fiziksel, sosyal, bilişsel, entelektüel, (opsiyonel) yaratıcı. */
@Entity(tableName = "kategori")
data class Kategori(
    @PrimaryKey val id: String,
    val ad: String,
    val emoji: String,
    val renkHex: String
)
