package com.beril.glowup.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Bir görevin tek bir uygulanma kaydı — retrieval practice notu dahil. */
@Entity(tableName = "oturum")
data class Oturum(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gorevId: String,
    val kategoriId: String,
    val baslangicZamani: Long,
    val bitisZamani: Long?,
    val tamamlandiMi: Boolean,
    val neOgrendinNotu: String? = null
)
