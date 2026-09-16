package com.beril.glowup.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.beril.glowup.data.model.EnerjiSeviyesi
import com.beril.glowup.data.model.Gorev
import com.beril.glowup.data.model.IlerlemeKaydi
import com.beril.glowup.data.model.Kategori
import com.beril.glowup.data.model.Oturum
import com.beril.glowup.data.model.SosyalMod
import com.beril.glowup.data.model.TekrarKarti

class Converters {
    @TypeConverter
    fun enerjiToString(v: EnerjiSeviyesi): String = v.name
    @TypeConverter
    fun stringToEnerji(v: String): EnerjiSeviyesi = EnerjiSeviyesi.valueOf(v)

    @TypeConverter
    fun sosyalToString(v: SosyalMod): String = v.name
    @TypeConverter
    fun stringToSosyal(v: String): SosyalMod = SosyalMod.valueOf(v)
}

@Database(
    entities = [Kategori::class, Gorev::class, Oturum::class, TekrarKarti::class, IlerlemeKaydi::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GlowUpDatabase : RoomDatabase() {
    abstract fun kategoriDao(): KategoriDao
    abstract fun gorevDao(): GorevDao
    abstract fun oturumDao(): OturumDao
    abstract fun tekrarKartiDao(): TekrarKartiDao
    abstract fun ilerlemeKaydiDao(): IlerlemeKaydiDao

    companion object {
        @Volatile private var INSTANCE: GlowUpDatabase? = null

        fun getInstance(context: Context): GlowUpDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GlowUpDatabase::class.java,
                    "glowup.db"
                ).build().also { INSTANCE = it }
            }
    }
}
