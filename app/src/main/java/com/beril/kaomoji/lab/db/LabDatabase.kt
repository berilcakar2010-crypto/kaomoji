package com.beril.kaomoji.lab.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.beril.kaomoji.lab.model.ContextEntity
import com.beril.kaomoji.lab.model.KnowledgeObjectEntity
import com.beril.kaomoji.lab.model.RelationshipEntity

/**
 * Lab 2.0'ın tek veritabanı. v1'in (eski assets/curriculum.json + filesDir/state.json)
 * tek seferlik aktarımı `CurriculumImporter`'ın işi — burada şema sadece v1'den başlar
 * (migration geçmişi yok çünkü bu, DB'nin ilk sürümü).
 */
@Database(
    entities = [KnowledgeObjectEntity::class, RelationshipEntity::class, ContextEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class LabDatabase : RoomDatabase() {
    abstract fun dao(): LabDao

    companion object {
        @Volatile private var instance: LabDatabase? = null

        fun get(context: Context): LabDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                LabDatabase::class.java,
                "lab.db",
            ).build().also { instance = it }
        }
    }
}
