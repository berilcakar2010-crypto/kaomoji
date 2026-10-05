package com.beril.kaomoji.lab.repository

import android.content.Context
import com.beril.kaomoji.lab.curriculum.CurriculumImporter
import com.beril.kaomoji.lab.curriculum.LegacyCurriculumAdapter
import com.beril.kaomoji.lab.db.LabDao
import com.beril.kaomoji.lab.db.LabDatabase
import com.beril.kaomoji.lab.model.KnowledgeObjectEntity
import com.beril.kaomoji.lab.model.ObjectKind
import com.beril.kaomoji.lab.model.RelationshipEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

/**
 * Lab 2.0'ın tek giriş noktası. Aşama 3'ten itibaren `ui/lab2/Lab2HomeScreen.kt` bunu
 * kullanıyor — eski ekranlar (GardenScreen, Store.kt vb.) hâlâ dokunulmadı, Lab 2.0 ekranı
 * mevcut uygulamanın yanında ayrı bir giriş noktası (Çanta → 🧪 Lab 2.0).
 */
class LabRepository(private val dao: LabDao) {

    constructor(context: Context) : this(LabDatabase.get(context).dao())

    private val importer = CurriculumImporter(dao)

    suspend fun getById(id: String): KnowledgeObjectEntity? = dao.getById(id)
    suspend fun getByKind(kind: ObjectKind): List<KnowledgeObjectEntity> = dao.getByKind(kind)
    fun observeAll(): Flow<List<KnowledgeObjectEntity>> = dao.observeAll()
    suspend fun search(query: String): List<KnowledgeObjectEntity> = dao.search(query)
    suspend fun relationshipsOf(objectId: String): List<RelationshipEntity> = dao.relationshipsOf(objectId)

    suspend fun pastTargetDate(today: LocalDate = LocalDate.now()): List<KnowledgeObjectEntity> =
        dao.pastTargetDate(today.toEpochDay())

    suspend fun upcoming(days: Long = 14, today: LocalDate = LocalDate.now()): List<KnowledgeObjectEntity> =
        dao.upcoming(today.toEpochDay(), today.plusDays(days).toEpochDay())

    /** Brain Inbox'ın yeni modeldeki karşılığı (§22) — sınıflandırma zorunlu değil, bu
     *  yüzden IDEA türünde, bağlamsız, zamanlamasız bir nesne olarak yazılır. Kullanıcı ya
     *  da AI (izin verilirse) sonradan türünü/bağlamını değiştirebilir — bu bir güncelleme,
     *  yeni bir nesne değil. */
    suspend fun quickCapture(title: String): String {
        val obj = KnowledgeObjectEntity(
            kind = ObjectKind.IDEA,
            title = title,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        dao.upsert(obj)
        return obj.id
    }

    /** Eski `assets/curriculum.json`'u (henüz contract v1 şeklinde değilse) adapte edip
     *  içe aktarır. Uygulama, müfredat üretim projesi contract v1'i doğrudan üretmeye
     *  başladığında bu metot yerini `import(rawContractJson)`'a bırakacak. */
    suspend fun importLegacyCurriculum(context: Context) {
        val raw = context.assets.open("curriculum.json").bufferedReader().use { it.readText() }
        importer.import(LegacyCurriculumAdapter.toContractPackage(raw))
    }

    companion object {
        fun forDao(dao: LabDao) = LabRepository(dao)
    }
}
