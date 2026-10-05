package com.beril.kaomoji.lab.repository

import android.content.Context
import com.beril.kaomoji.lab.curriculum.CurriculumImporter
import com.beril.kaomoji.lab.curriculum.LegacyCurriculumAdapter
import com.beril.kaomoji.lab.db.LabDao
import com.beril.kaomoji.lab.db.LabDatabase
import com.beril.kaomoji.lab.learning.LearningDiscipline
import com.beril.kaomoji.lab.learning.LearningSessionState
import com.beril.kaomoji.lab.model.KnowledgeObjectEntity
import com.beril.kaomoji.lab.model.LearningSessionPayload
import com.beril.kaomoji.lab.model.ObjectKind
import com.beril.kaomoji.lab.model.RelationshipEntity
import com.beril.kaomoji.lab.model.RelationshipType
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

    /** Elle, müfredat beklemeden bir kavram oluşturur — müfredat içe aktarma akışının
     *  dışında da öğrenme oturumu çalıştırılabilsin diye (§15 — kavramlar müfredata bağımlı
     *  değil, müfredat sadece onları toplu üretmenin bir yolu). */
    suspend fun createConcept(title: String, body: String? = null): String {
        val obj = KnowledgeObjectEntity(
            kind = ObjectKind.CONCEPT, title = title, body = body,
            createdAt = Instant.now(), updatedAt = Instant.now(),
        )
        dao.upsert(obj)
        return obj.id
    }

    /** Bir LEARNING_SESSION nesnesi oluşturur ve ilgili kavrama REINFORCES ile bağlar.
     *  Boş bir deneme metniyle başlar — kullanıcı hiç yazmasa bile oturum kalıcıdır. */
    suspend fun startLearningSession(conceptId: String, discipline: LearningDiscipline): String {
        val state = LearningSessionState(discipline)
        val obj = KnowledgeObjectEntity(
            kind = ObjectKind.LEARNING_SESSION,
            title = "Öğrenme oturumu",
            payload = LearningSessionPayload(discipline, state.stageIndex, state.attemptText, state.completed).toJson(),
            createdAt = Instant.now(), updatedAt = Instant.now(),
        )
        dao.upsert(obj)
        dao.upsertRelationship(RelationshipEntity(fromId = obj.id, toId = conceptId, type = RelationshipType.REINFORCES))
        return obj.id
    }

    suspend fun getLearningSessionState(sessionId: String): LearningSessionState? {
        val obj = dao.getById(sessionId) ?: return null
        val p = LearningSessionPayload.fromJson(obj.payload)
        return LearningSessionState(p.discipline, stageIndex = p.stageIndex, attemptText = p.attemptText, completed = p.completed)
    }

    /** Oturumun güncel durumunu olduğu gibi kalıcı hale getirir — her `advance()` sonrası
     *  çağrılmalı ki kullanıcının denemesi ve ilerlediği aşama hiçbir zaman kaybolmasın. */
    suspend fun saveLearningSessionState(sessionId: String, state: LearningSessionState) {
        val obj = dao.getById(sessionId) ?: return
        dao.update(
            obj.copy(
                payload = LearningSessionPayload(state.discipline, state.stageIndex, state.attemptText, state.completed).toJson(),
                updatedAt = Instant.now(),
            )
        )
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
