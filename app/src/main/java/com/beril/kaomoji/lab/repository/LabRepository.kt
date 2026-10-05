package com.beril.kaomoji.lab.repository

import android.content.Context
import com.beril.kaomoji.lab.curriculum.CurriculumImporter
import com.beril.kaomoji.lab.curriculum.ExternalCurriculumAdapter
import com.beril.kaomoji.lab.curriculum.LegacyCurriculumAdapter
import com.beril.kaomoji.lab.db.LabDao
import com.beril.kaomoji.lab.db.LabDatabase
import com.beril.kaomoji.lab.export.LabDataExport
import com.beril.kaomoji.lab.learning.LearningDiscipline
import com.beril.kaomoji.lab.learning.LearningSessionState
import com.beril.kaomoji.lab.model.FlashcardPayload
import com.beril.kaomoji.lab.model.KnowledgeObjectEntity
import com.beril.kaomoji.lab.model.LearningSessionPayload
import com.beril.kaomoji.lab.model.MistakePayload
import com.beril.kaomoji.lab.model.ObjectKind
import com.beril.kaomoji.lab.model.RelationshipEntity
import com.beril.kaomoji.lab.model.RelationshipType
import com.beril.kaomoji.lab.srs.SM2Engine
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

    /** `assets/lab2_curriculum.json` — 123 öğrenme nesnesi, gerçek önkoşul grafiği, bilingual
     *  başlıklar, soru bankaları, ustalık kanıtı. Contract v1'den daha zengin bir dış şema;
     *  [ExternalCurriculumAdapter] bunu doğrudan Room grafiğine çevirir. Yeniden çağırmak
     *  güvenli — sourcePackageId'ye göre silinip yeniden yazılır, çoğalmaz. */
    suspend fun importExternalCurriculum(context: Context) {
        val raw = context.assets.open("lab2_curriculum.json").bufferedReader().use { it.readText() }
        ExternalCurriculumAdapter.import(raw, dao)
    }

    /** Bu paketten gelen nesne sayısı — içe aktarma gerçekten olmuş mu, kaç nesne var,
     *  bir "İçe Aktarıldı (123)" göstergesi için. */
    suspend fun countFromPackage(packageId: String): Int = dao.getBySourcePackage(packageId).size

    // ── Hata Defteri (§24) ──
    /** Her hata: soru/deneme/ne-yanlış-gitti/neden/doğru-akıl-yürütme/kategori. Veri — suçlama
     *  değil. `conceptId` verilirse CAUSED_BY ile o kavrama bağlanır (hangi kavramdaki boşluk
     *  bu hataya yol açtı). */
    suspend fun logMistake(payload: MistakePayload, conceptId: String? = null): String {
        val obj = KnowledgeObjectEntity(
            kind = ObjectKind.MISTAKE,
            title = payload.problem,
            payload = payload.toJson(),
            createdAt = Instant.now(), updatedAt = Instant.now(),
        )
        dao.upsert(obj)
        if (conceptId != null) {
            dao.upsertRelationship(RelationshipEntity(fromId = conceptId, toId = obj.id, type = RelationshipType.CAUSED_BY))
        }
        return obj.id
    }

    suspend fun recentMistakes(limit: Int = 50): List<KnowledgeObjectEntity> =
        dao.getByKind(ObjectKind.MISTAKE).sortedByDescending { it.createdAt }.take(limit)

    /** Tekrarlayan kategori — "3 hata X kategorisinde" tespiti için. Suçlamadan, sadece sayar. */
    suspend fun mistakeCategoryCounts(): Map<String, Int> =
        dao.getByKind(ObjectKind.MISTAKE)
            .mapNotNull { runCatching { MistakePayload.fromJson(it.payload) }.getOrNull()?.category }
            .groupingBy { it }.eachCount()

    // ── Tekrar Kartları / SM-2 (§23) ──
    suspend fun createFlashcard(front: String, back: String, conceptId: String? = null): String {
        val obj = KnowledgeObjectEntity(
            kind = ObjectKind.FLASHCARD,
            title = front,
            payload = FlashcardPayload(front, back).toJson(),
            createdAt = Instant.now(), updatedAt = Instant.now(),
        )
        dao.upsert(obj)
        if (conceptId != null) {
            dao.upsertRelationship(RelationshipEntity(fromId = obj.id, toId = conceptId, type = RelationshipType.REINFORCES))
        }
        return obj.id
    }

    suspend fun allFlashcards(): List<KnowledgeObjectEntity> = dao.getByKind(ObjectKind.FLASHCARD)

    suspend fun dueFlashcards(today: LocalDate = LocalDate.now()): List<KnowledgeObjectEntity> =
        allFlashcards().filter { card ->
            val p = runCatching { FlashcardPayload.fromJson(card.payload) }.getOrNull()
            p == null || p.nextReviewEpochDay <= today.toEpochDay()
        }

    /** Bir kartı inceler, SM-2 ile yeni durumu hesaplar ve kalıcı hale getirir. Kartın "neden
     *  tekrar edildiğini" kullanıcı her zaman anlasın diye — bu metot kartı asla tekrarın
     *  tamamını tanımlayan tek şey yapmaz, sadece bir bileşendir (§23). */
    suspend fun reviewFlashcard(cardId: String, quality: Int, today: LocalDate = LocalDate.now()) {
        val obj = dao.getById(cardId) ?: return
        val payload = runCatching { FlashcardPayload.fromJson(obj.payload) }.getOrDefault(FlashcardPayload(obj.title, ""))
        val updated = SM2Engine.review(payload, quality, today.toEpochDay())
        dao.update(obj.copy(payload = updated.toJson(), updatedAt = Instant.now()))
    }

    // ── Veri sahipliği: dışa/içe aktarma (§34/§41) ──
    /** Kullanıcının ürettiği HER şeyi (bir müfredat paketi değil — kendi verisi) tek bir
     *  JSON string olarak döner. Çağıran taraf bunu SAF ile bir dosyaya yazar. */
    suspend fun exportAll(): String = LabDataExport.export(
        dao.getAllOnce(), dao.getAllRelationshipsOnce(), dao.getAllContextsOnce(),
    )

    /** Dışa aktarılmış bir JSON'u geri yükler. Var olan bir id'yi EZER (REPLACE) — mevcut
     *  verinin üzerine bilerek yazdığını bilerek kabul etmiş olman gerekir; bu metot sessizce
     *  birleştirmez ya da silmez, sadece verilenleri yazar. */
    suspend fun importAll(raw: String): LabDataExport.ImportResult {
        val result = LabDataExport.import(raw)
        dao.upsertAll(result.objects)
        dao.upsertRelationships(result.relationships)
        result.contexts.forEach { dao.upsertContext(it) }
        return result
    }

    companion object {
        fun forDao(dao: LabDao) = LabRepository(dao)
    }
}
