package com.beril.kaomoji.lab.repository

import android.content.Context
import com.beril.kaomoji.lab.curriculum.CurriculumImporter
import com.beril.kaomoji.lab.curriculum.ExternalCurriculumAdapter
import com.beril.kaomoji.lab.curriculum.LegacyCurriculumAdapter
import com.beril.kaomoji.lab.db.LabDao
import com.beril.kaomoji.lab.db.LabDatabase
import com.beril.kaomoji.lab.export.LabDataExport
import com.beril.kaomoji.lab.migration.LegacyDataMigrator
import com.beril.kaomoji.lab.migration.MigrationSummary
import com.beril.kaomoji.lab.learning.LearningDiscipline
import com.beril.kaomoji.lab.learning.LearningSessionState
import com.beril.kaomoji.lab.model.ExamPayload
import com.beril.kaomoji.lab.model.ExplanationPayload
import com.beril.kaomoji.lab.model.FlashcardPayload
import com.beril.kaomoji.lab.model.KnowledgeObjectEntity
import com.beril.kaomoji.lab.model.LearningSessionPayload
import com.beril.kaomoji.lab.model.MistakePayload
import com.beril.kaomoji.lab.model.ObjectKind
import com.beril.kaomoji.lab.model.ProjectPayload
import com.beril.kaomoji.lab.model.Schedule
import com.beril.kaomoji.lab.model.ScheduleStatus
import com.beril.kaomoji.lab.model.RelationshipEntity
import com.beril.kaomoji.lab.model.RelationshipType
import com.beril.kaomoji.lab.srs.SM2Engine
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

/**
 * Uygulamanın bilgi grafiğine tek giriş noktası — `ui.lab2` paketindeki ekranlar bunu kullanır.
 * Eski `Store.kt` yalnızca `migrateLegacyData` için salt okunur bir göç kaynağı olarak kalır.
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

    /** Lab artık tek uygulama — kişisel müfredat paketi bir buton beklemeden, uygulama ilk
     *  açıldığında kendiliğinden hazır olmalı. Paket zaten içe aktarılmışsa (sayı > 0) hiçbir
     *  şey yapmaz; `importExternalCurriculum` zaten idempotent (sourcePackageId'ye göre
     *  REPLACE), bu yüzden burada tekrar tekrar çağırmak da güvenli. */
    suspend fun ensureDefaultCurriculumImported(context: Context) {
        if (countFromPackage("lab2-personal-curriculum") == 0) {
            importExternalCurriculum(context)
        }
    }

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

    // ── Projeler (§21) ──
    suspend fun createProject(title: String, researchQuestion: String): String {
        val obj = KnowledgeObjectEntity(
            kind = ObjectKind.PROJECT,
            title = title,
            payload = ProjectPayload(researchQuestion).toJson(),
            createdAt = Instant.now(), updatedAt = Instant.now(),
        )
        dao.upsert(obj)
        return obj.id
    }

    suspend fun allProjects(): List<KnowledgeObjectEntity> = dao.getByKind(ObjectKind.PROJECT)

    /** Projenin sadece notes/hypothesis/nextAction/status alanlarını günceller — başlık ve
     *  researchQuestion sabit kalır, bir proje zamanla "ne sorduğunu" unutmamalı. */
    suspend fun updateProject(projectId: String, notes: String? = null, hypothesis: String? = null, nextAction: String? = null, status: String? = null) {
        val obj = dao.getById(projectId) ?: return
        val p = runCatching { ProjectPayload.fromJson(obj.payload) }.getOrNull() ?: return
        val updated = p.copy(
            notes = notes ?: p.notes,
            hypothesis = hypothesis ?: p.hypothesis,
            nextAction = nextAction ?: p.nextAction,
            status = status ?: p.status,
        )
        dao.update(obj.copy(payload = updated.toJson(), updatedAt = Instant.now()))
    }

    // ── Sınavlar / Ödevler (§20) ──
    suspend fun createExam(title: String, scope: String, examDate: LocalDate? = null, isAssignment: Boolean = false): String {
        val obj = KnowledgeObjectEntity(
            kind = if (isAssignment) ObjectKind.ASSIGNMENT else ObjectKind.EXAM,
            title = title,
            payload = ExamPayload(scope).toJson(),
            schedule = examDate?.let { Schedule(examDate = it.toEpochDay(), status = ScheduleStatus.SCHEDULED) },
            createdAt = Instant.now(), updatedAt = Instant.now(),
        )
        dao.upsert(obj)
        return obj.id
    }

    suspend fun allExams(): List<KnowledgeObjectEntity> =
        (dao.getByKind(ObjectKind.EXAM) + dao.getByKind(ObjectKind.ASSIGNMENT)).sortedBy { it.schedule?.examDate }

    suspend fun updateExamPrepStatus(examId: String, prepStatus: String) {
        val obj = dao.getById(examId) ?: return
        val p = runCatching { ExamPayload.fromJson(obj.payload) }.getOrNull() ?: return
        dao.update(obj.copy(payload = p.copy(prepStatus = prepStatus).toJson(), updatedAt = Instant.now()))
    }

    /** Bu sınav/ödevin kapsadığı kavramlar — ConceptGraph'tan ASSESSES ilişkisiyle (müfredat
     *  paketinden gelmişse) ya da elle eklenmiş olabilir. "Bu sınav için ne önemli?" (§20). */
    suspend fun conceptsAssessedBy(examId: String): List<KnowledgeObjectEntity> =
        dao.relationshipsOf(examId)
            .filter { it.type == RelationshipType.ASSESSES && it.fromId == examId }
            .mapNotNull { dao.getById(it.toId) }

    // ── Anlatım arşivi / Feynman tekniği (§44 kaydı, §9 ilişkisi) ──
    /** Bir kavram için gerçek bir ses kaydı (kendi sesinle, defter/kitaba bakmadan anlatma)
     *  oluşturur ve EXPLAINS ilişkisiyle o kavrama bağlar. `audioFilePath` [LabRecorder]'ın
     *  ürettiği dosyanın mutlak yolu. */
    suspend fun createExplanation(conceptId: String, conceptTitle: String, audioFilePath: String, language: String = "tr"): String {
        val obj = KnowledgeObjectEntity(
            kind = ObjectKind.EXPLANATION,
            title = "$conceptTitle — anlatım",
            payload = ExplanationPayload(language = language, audioFilePath = audioFilePath).toJson(),
            createdAt = Instant.now(), updatedAt = Instant.now(),
        )
        dao.upsert(obj)
        dao.upsertRelationship(RelationshipEntity(fromId = obj.id, toId = conceptId, type = RelationshipType.EXPLAINS))
        return obj.id
    }

    /** Bir kavram için kaydedilmiş tüm anlatımlar, en yeniden en eskiye. */
    suspend fun explanationsFor(conceptId: String): List<KnowledgeObjectEntity> =
        dao.relationshipsOf(conceptId)
            .filter { it.type == RelationshipType.EXPLAINS && it.toId == conceptId }
            .mapNotNull { dao.getById(it.fromId) }
            .sortedByDescending { it.createdAt }

    /** AI'nin bir kaydı transkribe etmesinin sonucunu kalıcı hale getirir — transkript metni
     *  kendi söylediğin şeyin mekanik bir yazıya dökümü (dilbilgisi/netlik yardımındaki gibi
     *  bir "anlamı değiştirme" riski yok), bu yüzden diğer yazım yardımlarından farklı olarak
     *  doğrudan kaydedilir; AiResultView yine de sonucu ayrı gösterir, şeffaflık için. */
    suspend fun attachTranscript(explanationId: String, transcript: String) {
        val obj = dao.getById(explanationId) ?: return
        dao.update(obj.copy(body = transcript, updatedAt = Instant.now()))
    }

    /** AI'nin bir transkript üzerindeki değerlendirmesini [ExplanationPayload.aiEvaluation]'a
     *  kaydeder. */
    suspend fun attachEvaluation(explanationId: String, evaluation: String) {
        val obj = dao.getById(explanationId) ?: return
        val payload = runCatching { ExplanationPayload.fromJson(obj.payload) }.getOrNull() ?: return
        dao.update(obj.copy(payload = payload.copy(aiEvaluation = evaluation).toJson(), updatedAt = Instant.now()))
    }

    /** Eski `Store.kt` verisini (hatalar, tekrar kartları, anlatımlar, Brain Inbox, projeler,
     *  sınavlar, pratik günlükleri, haftalık değerlendirmeler) Lab 2.0'ın grafiğine kopyalar.
     *  Store.kt'ye hiçbir yazma yapılmaz — salt okunur bir geçiş. Yeniden çağırmak güvenli
     *  (sourcePackageId ile silinip yeniden yazılır, çoğalmaz). `done`/`dailyLogs`/`problems`
     *  gibi eski müfredata özgü ince taneli kayıtlar taşınmıyor — bkz. LegacyDataMigrator'ın
     *  dosya başındaki not. */
    suspend fun migrateLegacyData(context: Context): MigrationSummary {
        val store = com.beril.kaomoji.data.Store(context)
        val objects = mutableListOf<KnowledgeObjectEntity>()

        objects += store.mistakes.map { LegacyDataMigrator.mapMistake(it) }
        objects += store.flashcards.map { LegacyDataMigrator.mapFlashcard(it) }
        objects += store.recordings.map { LegacyDataMigrator.mapRecording(it) }
        objects += store.inbox.map { LegacyDataMigrator.mapInboxNote(it) }
        objects += store.problems.map { LegacyDataMigrator.mapProblemLog(it) }
        objects += store.reviews.map { LegacyDataMigrator.mapWeeklyReview(it) }
        objects += store.curriculum.projects.mapNotNull { def ->
            store.projectStates[def.id]?.let { LegacyDataMigrator.mapProject(def, it) }
        }
        objects += store.curriculum.assessments.mapNotNull { def ->
            store.assessmentStates[def.id]?.let { LegacyDataMigrator.mapAssessment(def, it) }
        }

        dao.importPackage(objects, emptyList(), LegacyDataMigrator.SOURCE_ID)

        return MigrationSummary(
            mistakes = store.mistakes.size,
            flashcards = store.flashcards.size,
            recordings = store.recordings.size,
            inboxNotes = store.inbox.size,
            problems = store.problems.size,
            reviews = store.reviews.size,
            projects = store.curriculum.projects.count { store.projectStates[it.id] != null },
            assessments = store.curriculum.assessments.count { store.assessmentStates[it.id] != null },
        )
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
