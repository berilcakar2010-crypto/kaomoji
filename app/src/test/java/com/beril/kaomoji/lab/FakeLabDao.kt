package com.beril.kaomoji.lab

import com.beril.kaomoji.lab.db.LabDao
import com.beril.kaomoji.lab.model.ContextEntity
import com.beril.kaomoji.lab.model.KnowledgeObjectEntity
import com.beril.kaomoji.lab.model.ObjectKind
import com.beril.kaomoji.lab.model.RelationshipEntity
import kotlinx.coroutines.flow.MutableStateFlow

/** Room'suz, bellek içi [LabDao] — birim testlerinde gerçek veritabanı kurmaya gerek kalmasın. */
class FakeLabDao : LabDao {
    val objects = linkedMapOf<String, KnowledgeObjectEntity>()
    val relationships = linkedMapOf<String, RelationshipEntity>()
    val contexts = linkedMapOf<String, ContextEntity>()
    private val allFlow = MutableStateFlow<List<KnowledgeObjectEntity>>(emptyList())

    private fun publish() { allFlow.value = objects.values.sortedByDescending { it.updatedAt } }

    override suspend fun upsert(obj: KnowledgeObjectEntity) { objects[obj.id] = obj; publish() }
    override suspend fun upsertAll(objs: List<KnowledgeObjectEntity>) { objs.forEach { objects[it.id] = it }; publish() }
    override suspend fun update(obj: KnowledgeObjectEntity) { objects[obj.id] = obj; publish() }
    override suspend fun delete(obj: KnowledgeObjectEntity) { objects.remove(obj.id); publish() }
    override suspend fun deleteById(id: String) { objects.remove(id); publish() }
    override suspend fun getById(id: String): KnowledgeObjectEntity? = objects[id]
    override fun observeAll() = allFlow
    override fun observeByKind(kind: ObjectKind) = MutableStateFlow(objects.values.filter { it.kind == kind })
    override suspend fun getByKind(kind: ObjectKind): List<KnowledgeObjectEntity> = objects.values.filter { it.kind == kind }
    override suspend fun search(q: String): List<KnowledgeObjectEntity> =
        objects.values.filter { it.title.contains(q, true) || (it.body?.contains(q, true) == true) }
    override suspend fun getBySourcePackage(packageId: String): List<KnowledgeObjectEntity> =
        objects.values.filter { it.sourcePackageId == packageId }
    override suspend fun deleteBySourcePackage(packageId: String) {
        objects.values.filter { it.sourcePackageId == packageId }.forEach { objects.remove(it.id) }
        publish()
    }
    override suspend fun pastTargetDate(todayEpochDay: Long): List<KnowledgeObjectEntity> =
        objects.values.filter { it.schedule?.targetDate != null && it.schedule.targetDate < todayEpochDay }
    override suspend fun upcoming(fromEpochDay: Long, toEpochDay: Long): List<KnowledgeObjectEntity> =
        objects.values.filter {
            val d = it.schedule?.examDate ?: it.schedule?.deadline
            d != null && d in fromEpochDay..toEpochDay
        }

    override suspend fun upsertRelationship(rel: RelationshipEntity) { relationships[rel.id] = rel }
    override suspend fun upsertRelationships(rels: List<RelationshipEntity>) { rels.forEach { relationships[it.id] = it } }
    override suspend fun deleteRelationship(rel: RelationshipEntity) { relationships.remove(rel.id) }
    override suspend fun relationshipsFrom(objectId: String) = relationships.values.filter { it.fromId == objectId }
    override suspend fun relationshipsTo(objectId: String) = relationships.values.filter { it.toId == objectId }
    override suspend fun relationshipsOf(objectId: String) = relationships.values.filter { it.fromId == objectId || it.toId == objectId }

    override suspend fun upsertContext(ctx: ContextEntity) { contexts[ctx.id] = ctx }
    override fun observeContexts() = MutableStateFlow(contexts.values.sortedBy { it.name })
}
