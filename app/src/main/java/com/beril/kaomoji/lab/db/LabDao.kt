package com.beril.kaomoji.lab.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.beril.kaomoji.lab.model.ContextEntity
import com.beril.kaomoji.lab.model.KnowledgeObjectEntity
import com.beril.kaomoji.lab.model.ObjectKind
import com.beril.kaomoji.lab.model.RelationshipEntity
import kotlinx.coroutines.flow.Flow

/**
 * Not: arama şu an basit bir LIKE sorgusu, FTS4 değil. Room'un FTS4 varyantı content-table
 * modeliyle bu entity'nin TypeConverter'lı/Embedded alanlarıyla iyi karışmıyor; binlerce
 * nesneye kadar LIKE + indeksli `kind`/`sourcePackageId` yeterli kalır (§36). Gerçekten büyük
 * veri setinde FTS4'e geçiş — şemayı bozmadan, sadece bu dosyada — bir sonraki aşamanın işi.
 */
@Dao
interface LabDao {
    // ── KnowledgeObject ──
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(obj: KnowledgeObjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(objs: List<KnowledgeObjectEntity>)

    @Update
    suspend fun update(obj: KnowledgeObjectEntity)

    @Delete
    suspend fun delete(obj: KnowledgeObjectEntity)

    @Query("DELETE FROM knowledge_objects WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM knowledge_objects WHERE id = :id")
    suspend fun getById(id: String): KnowledgeObjectEntity?

    @Query("SELECT * FROM knowledge_objects ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<KnowledgeObjectEntity>>

    @Query("SELECT * FROM knowledge_objects")
    suspend fun getAllOnce(): List<KnowledgeObjectEntity>

    @Query("SELECT * FROM knowledge_objects WHERE kind = :kind ORDER BY updatedAt DESC")
    fun observeByKind(kind: ObjectKind): Flow<List<KnowledgeObjectEntity>>

    @Query("SELECT * FROM knowledge_objects WHERE kind = :kind ORDER BY updatedAt DESC")
    suspend fun getByKind(kind: ObjectKind): List<KnowledgeObjectEntity>

    @Query("SELECT * FROM knowledge_objects WHERE title LIKE '%' || :q || '%' OR body LIKE '%' || :q || '%' ORDER BY updatedAt DESC LIMIT 100")
    suspend fun search(q: String): List<KnowledgeObjectEntity>

    @Query("SELECT * FROM knowledge_objects WHERE sourcePackageId = :packageId")
    suspend fun getBySourcePackage(packageId: String): List<KnowledgeObjectEntity>

    @Query("DELETE FROM knowledge_objects WHERE sourcePackageId = :packageId")
    suspend fun deleteBySourcePackage(packageId: String)

    /** "scheduleStatus henüz COMPLETED/SKIPPED değil ve targetDate/deadline bugünden önce"
     *  — bir backlog tablosu değil, salt bir sorgu. Advisor modu bunu okuyup seçenek sunar,
     *  sistem kendi başına hiçbir şeyi "gecikmiş" ilan etmez (§18). */
    @Query(
        """
        SELECT * FROM knowledge_objects
        WHERE sched_status NOT IN ('COMPLETED', 'SKIPPED')
          AND sched_targetDate IS NOT NULL AND sched_targetDate < :todayEpochDay
        ORDER BY sched_targetDate ASC
        """
    )
    suspend fun pastTargetDate(todayEpochDay: Long): List<KnowledgeObjectEntity>

    @Query(
        """
        SELECT * FROM knowledge_objects
        WHERE sched_status NOT IN ('COMPLETED', 'SKIPPED')
          AND (sched_examDate BETWEEN :fromEpochDay AND :toEpochDay
               OR sched_deadline BETWEEN :fromEpochDay AND :toEpochDay)
        ORDER BY COALESCE(sched_examDate, sched_deadline) ASC
        """
    )
    suspend fun upcoming(fromEpochDay: Long, toEpochDay: Long): List<KnowledgeObjectEntity>

    // ── Relationship ──
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRelationship(rel: RelationshipEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRelationships(rels: List<RelationshipEntity>)

    @Delete
    suspend fun deleteRelationship(rel: RelationshipEntity)

    @Query("SELECT * FROM relationships WHERE fromId = :objectId")
    suspend fun relationshipsFrom(objectId: String): List<RelationshipEntity>

    @Query("SELECT * FROM relationships WHERE toId = :objectId")
    suspend fun relationshipsTo(objectId: String): List<RelationshipEntity>

    @Query("SELECT * FROM relationships WHERE fromId = :objectId OR toId = :objectId")
    suspend fun relationshipsOf(objectId: String): List<RelationshipEntity>

    @Query("SELECT * FROM relationships")
    suspend fun getAllRelationshipsOnce(): List<RelationshipEntity>

    // ── Context ──
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContext(ctx: ContextEntity)

    @Query("SELECT * FROM contexts ORDER BY name ASC")
    fun observeContexts(): Flow<List<ContextEntity>>

    @Query("SELECT * FROM contexts")
    suspend fun getAllContextsOnce(): List<ContextEntity>

    @Transaction
    suspend fun importPackage(
        objects: List<KnowledgeObjectEntity>,
        relationships: List<RelationshipEntity>,
        sourcePackageId: String,
    ) {
        deleteBySourcePackage(sourcePackageId)
        upsertAll(objects)
        upsertRelationships(relationships)
    }
}
