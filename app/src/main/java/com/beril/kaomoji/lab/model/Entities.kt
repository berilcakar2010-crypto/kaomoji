package com.beril.kaomoji.lab.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

/**
 * Opsiyonel zamanlama. Hiçbir alan zorunlu değil — bir KnowledgeObjectEntity bunsuz da
 * tamamen geçerlidir. "suggestedDate geçti" asla "müfredat bozuldu" anlamına gelmez; Advisor
 * modu bunu bir seçenek listesi olarak sunar (§18), sistemin kendisi bunu hiç bilmez.
 */
data class Schedule(
    val suggestedDate: Long? = null,   // epoch day (LocalDate.toEpochDay) — kolaylık için
    val targetDate: Long? = null,
    val deadline: Long? = null,
    val examDate: Long? = null,
    val priority: Int? = null,
    val estimatedEffortMin: Int? = null,
    val status: ScheduleStatus = ScheduleStatus.SUGGESTED,
)

/**
 * Lab 2.0'ın tek temel nesnesi. Spec §15'in istediği "en küçük tutarlı model" — her nesne tipi
 * (Concept/Question/Mistake/Project/Exam/...) burada `kind` değeri, ayrı tablo değil.
 *
 * `payload` türe özgü yapılandırılmış alanları tutar (örn. MISTAKE için
 * {"attempt":"...","whatWentWrong":"...","correctReasoning":"..."}, FLASHCARD için SM-2 durumu
 * {"easeFactor":2.5,"repetitions":3,"intervalDays":6,"nextReviewEpochDay":19980}) — ham JSON
 * string olarak saklanır çünkü tür başına ayrı sütun/tablo şişirmek bu modelin amacını
 * (tek motor, her tür) baltalar. Okuma tarafı (ör. `MistakePayload`, `FlashcardPayload` küçük
 * data class'ları + org.json) bunu ayrıştırır.
 */
@Entity(
    tableName = "knowledge_objects",
    indices = [Index("kind"), Index("sourcePackageId")],
)
data class KnowledgeObjectEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val kind: ObjectKind,
    val title: String,
    val body: String? = null,
    val contextIds: Set<String> = emptySet(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    @Embedded(prefix = "sched_") val schedule: Schedule? = null,
    val payload: String = "{}",
    /** Bu nesne bir müfredat paketinden geldiyse onun id'si — paket yeniden içe aktarılırken
     *  (veya kaldırılırken) "bu kaynaktan gelen her şeyi sil" sorgusunu tek koşulla yapabilmek
     *  için. El ile oluşturulan nesnelerde null. */
    val sourcePackageId: String? = null,
)

@Entity(
    tableName = "relationships",
    indices = [Index("fromId"), Index("toId"), Index("type")],
    foreignKeys = [
        ForeignKey(
            entity = KnowledgeObjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["fromId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = KnowledgeObjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["toId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class RelationshipEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val fromId: String,
    val toId: String,
    val type: RelationshipType,
    val note: String? = null,
)

/** Okul / AP / Olimpiyat / Araştırma / Proje X gibi akademik bağlamlar. Bir Concept birden
 *  fazla bağlama ait olabilir — kopyalanmaz, sadece contextIds'e eklenir (§19). */
@Entity(tableName = "contexts")
data class ContextEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val kind: ContextKind,
)
