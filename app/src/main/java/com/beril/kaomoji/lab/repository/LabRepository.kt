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
import java.time.LocalDate

/**
 * Lab 2.0'ın tek giriş noktası. Henüz hiçbir ekran bunu kullanmıyor (bu Aşama 1'in kapsamı
 * dışında — ekranların yeni modele taşınması sonraki aşamanın işi); bu aşamada amaç
 * domain model + Room + curriculum contract/importer'ın gerçekten derlendiğini ve
 * gerçekten çalıştığını (birim testleriyle) kanıtlamak.
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
