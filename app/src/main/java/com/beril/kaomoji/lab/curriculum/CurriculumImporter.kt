package com.beril.kaomoji.lab.curriculum

import com.beril.kaomoji.lab.db.LabDao
import com.beril.kaomoji.lab.model.ContextEntity
import com.beril.kaomoji.lab.model.ContextKind
import com.beril.kaomoji.lab.model.KnowledgeObjectEntity
import com.beril.kaomoji.lab.model.ObjectKind
import com.beril.kaomoji.lab.model.RelationshipEntity
import com.beril.kaomoji.lab.model.RelationshipType
import com.beril.kaomoji.lab.model.Schedule
import com.beril.kaomoji.lab.model.ScheduleStatus
import java.time.Instant

/**
 * Bir [CurriculumPackage]'ı KnowledgeObjectEntity/RelationshipEntity satırlarına çevirir ve
 * `sourcePackageId` ile etiketleyip yazar. Aynı packageId ile yeniden içe aktarma = eski
 * satırları sil + yenilerini yaz (§17: "müfredat tamamen değiştirilebilir olmalı") — elle
 * yazılmış bir migration değil, tek bir transaction.
 */
class CurriculumImporter(private val dao: LabDao) {

    suspend fun import(pkg: CurriculumPackage) {
        val now = Instant.now()
        val objects = mutableListOf<KnowledgeObjectEntity>()
        val relationships = mutableListOf<RelationshipEntity>()

        // Course -> COURSE nesnesi
        pkg.courses.forEach { c ->
            objects += KnowledgeObjectEntity(
                id = c.id,
                kind = ObjectKind.COURSE,
                title = c.name,
                contextIds = c.contexts.toSet(),
                createdAt = now,
                updatedAt = now,
                sourcePackageId = pkg.packageId,
            )
        }

        // Unit -> CURRICULUM_UNIT nesnesi, course'a PREREQUISITE_OF/RELATED_TO değil
        // doğrudan "parça" ilişkisi — USED_IN ile course'a bağlanır, önkoşul birimlere
        // PREREQUISITE_OF ile.
        pkg.units.forEach { u ->
            objects += KnowledgeObjectEntity(
                id = u.id,
                kind = ObjectKind.CURRICULUM_UNIT,
                title = u.title,
                body = u.objectives.joinToString("\n") { "- $it" }.ifBlank { null },
                createdAt = now,
                updatedAt = now,
                schedule = u.suggestedDateEpochDay?.let {
                    Schedule(suggestedDate = it, status = ScheduleStatus.SUGGESTED)
                },
                sourcePackageId = pkg.packageId,
            )
            relationships += RelationshipEntity(fromId = u.id, toId = u.courseId, type = RelationshipType.USED_IN)
            u.prerequisiteUnitIds.forEach { prereqId ->
                relationships += RelationshipEntity(fromId = prereqId, toId = u.id, type = RelationshipType.PREREQUISITE_OF)
            }
        }

        // Concept -> CONCEPT nesnesi, biriminin parçası (USED_IN)
        pkg.concepts.forEach { c ->
            objects += KnowledgeObjectEntity(
                id = c.id,
                kind = ObjectKind.CONCEPT,
                title = c.title,
                body = c.body,
                createdAt = now,
                updatedAt = now,
                sourcePackageId = pkg.packageId,
            )
            relationships += RelationshipEntity(fromId = c.id, toId = c.unitId, type = RelationshipType.USED_IN)
        }

        // Question -> QUESTION nesnesi, test ettiği concept'lere TESTS ile bağlanır
        pkg.questions.forEach { q ->
            objects += KnowledgeObjectEntity(
                id = q.id,
                kind = ObjectKind.QUESTION,
                title = q.prompt,
                body = q.answer,
                payload = """{"questionKind":"${q.kind}"}""",
                createdAt = now,
                updatedAt = now,
                sourcePackageId = pkg.packageId,
            )
            q.conceptIds.forEach { conceptId ->
                relationships += RelationshipEntity(fromId = q.id, toId = conceptId, type = RelationshipType.TESTS)
            }
        }

        // Assessment -> EXAM/ASSIGNMENT nesnesi, kapsadığı concept'lere ASSESSES ile bağlanır
        pkg.assessments.forEach { a ->
            objects += KnowledgeObjectEntity(
                id = a.id,
                kind = if (a.kind == "ASSIGNMENT") ObjectKind.ASSIGNMENT else ObjectKind.EXAM,
                title = a.kind,
                createdAt = now,
                updatedAt = now,
                schedule = a.suggestedDateEpochDay?.let { Schedule(examDate = it, status = ScheduleStatus.SCHEDULED) },
                sourcePackageId = pkg.packageId,
            )
            a.scopeConceptIds.forEach { conceptId ->
                relationships += RelationshipEntity(fromId = a.id, toId = conceptId, type = RelationshipType.ASSESSES)
            }
        }

        // Resource -> RESOURCE nesnesi
        pkg.resources.forEach { r ->
            objects += KnowledgeObjectEntity(
                id = r.id,
                kind = ObjectKind.RESOURCE,
                title = r.title,
                body = listOfNotNull(r.url, r.note).joinToString("\n").ifBlank { null },
                createdAt = now,
                updatedAt = now,
                sourcePackageId = pkg.packageId,
            )
        }

        // Sözleşmedeki açık ilişkiler
        pkg.relationships.forEach { r ->
            val type = runCatching { RelationshipType.valueOf(r.type) }.getOrNull()
            if (type != null) {
                relationships += RelationshipEntity(fromId = r.fromId, toId = r.toId, type = type, note = r.note)
            }
        }

        dao.importPackage(objects, relationships, pkg.packageId)

        pkg.domains.forEach { d ->
            dao.upsertContext(ContextEntity(id = d.id, name = d.name, kind = ContextKind.OTHER))
        }
    }
}
