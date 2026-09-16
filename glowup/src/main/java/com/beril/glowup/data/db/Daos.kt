package com.beril.glowup.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.beril.glowup.data.model.Gorev
import com.beril.glowup.data.model.IlerlemeKaydi
import com.beril.glowup.data.model.Kategori
import com.beril.glowup.data.model.Oturum
import com.beril.glowup.data.model.TekrarKarti

@Dao
interface KategoriDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun hepsiniEkle(kategoriler: List<Kategori>)

    @Query("SELECT * FROM kategori")
    suspend fun hepsi(): List<Kategori>
}

@Dao
interface GorevDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun hepsiniEkle(gorevler: List<Gorev>)

    @Query("SELECT * FROM gorev WHERE kategoriId = :kategoriId AND sureDk <= :maxSureDk")
    suspend fun kategoriVeSureyeGore(kategoriId: String, maxSureDk: Int): List<Gorev>

    @Query("UPDATE gorev SET sonYapilmaZamani = :zaman WHERE id = :gorevId")
    suspend fun sonYapilmaZamaniGuncelle(gorevId: String, zaman: Long)
}

@Dao
interface OturumDao {
    @Insert
    suspend fun ekle(oturum: Oturum): Long

    @Query("SELECT * FROM oturum WHERE kategoriId = :kategoriId ORDER BY baslangicZamani DESC LIMIT :limit")
    suspend fun sonOturumlar(kategoriId: String, limit: Int): List<Oturum>
}

@Dao
interface TekrarKartiDao {
    @Insert
    suspend fun ekle(kart: TekrarKarti): Long

    @Query("SELECT * FROM tekrar_karti WHERE sonrakiGosterimZamani <= :simdi ORDER BY sonrakiGosterimZamani ASC")
    suspend fun gosterimZamaniGelenler(simdi: Long): List<TekrarKarti>

    @Query("UPDATE tekrar_karti SET kolaylikFaktoru = :kf, tekrarSayisi = :tekrar, araligGun = :aralik, sonrakiGosterimZamani = :sonraki, sonZorlukPuani = :puan WHERE id = :id")
    suspend fun guncelle(id: Long, kf: Float, tekrar: Int, aralik: Int, sonraki: Long, puan: Int)
}

@Dao
interface IlerlemeKaydiDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun kaydet(kayit: IlerlemeKaydi)

    @Query("SELECT * FROM ilerleme_kaydi WHERE gunDamgasi >= :baslangic ORDER BY gunDamgasi ASC")
    suspend fun aralikta(baslangic: Long): List<IlerlemeKaydi>
}
