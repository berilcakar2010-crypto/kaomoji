package com.beril.kaomoji.lab.export

import com.beril.kaomoji.lab.model.ContextEntity
import com.beril.kaomoji.lab.model.ContextKind
import com.beril.kaomoji.lab.model.KnowledgeObjectEntity
import com.beril.kaomoji.lab.model.ObjectKind
import com.beril.kaomoji.lab.model.RelationshipEntity
import com.beril.kaomoji.lab.model.RelationshipType
import com.beril.kaomoji.lab.model.Schedule
import com.beril.kaomoji.lab.model.ScheduleStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class LabDataExportTest {

    @Test
    fun `an object with a schedule and contexts round-trips exactly`() {
        val now = Instant.parse("2027-01-01T00:00:00Z")
        val original = KnowledgeObjectEntity(
            id = "o1", kind = ObjectKind.MISTAKE, title = "Başlık", body = "Gövde",
            contextIds = setOf("school", "ap"), createdAt = now, updatedAt = now,
            schedule = Schedule(targetDate = 123L, priority = 2, status = ScheduleStatus.TARGET_SET),
            payload = """{"category":"sign-error"}""", sourcePackageId = "pkg-1",
        )

        val json = LabDataExport.export(listOf(original), emptyList(), emptyList())
        val result = LabDataExport.import(json)

        val restored = result.objects.single()
        assertEquals(original.id, restored.id)
        assertEquals(original.kind, restored.kind)
        assertEquals(original.title, restored.title)
        assertEquals(original.body, restored.body)
        assertEquals(original.contextIds, restored.contextIds)
        assertEquals(original.createdAt, restored.createdAt)
        assertEquals(original.schedule?.targetDate, restored.schedule?.targetDate)
        assertEquals(original.schedule?.priority, restored.schedule?.priority)
        assertEquals(original.schedule?.status, restored.schedule?.status)
        assertEquals(original.payload, restored.payload)
        assertEquals(original.sourcePackageId, restored.sourcePackageId)
    }

    @Test
    fun `an object with no schedule stays scheduleless after round-trip (never fabricated)`() {
        val now = Instant.now()
        val original = KnowledgeObjectEntity(id = "o2", kind = ObjectKind.IDEA, title = "Fikir", createdAt = now, updatedAt = now)

        val result = LabDataExport.import(LabDataExport.export(listOf(original), emptyList(), emptyList()))

        assertNull(result.objects.single().schedule)
    }

    @Test
    fun `relationships and contexts round-trip with all fields intact`() {
        val rel = RelationshipEntity(id = "r1", fromId = "a", toId = "b", type = RelationshipType.PREREQUISITE_OF, note = "hard")
        val ctx = ContextEntity(id = "c1", name = "Fizik Olimpiyatı", kind = ContextKind.OLYMPIAD)

        val result = LabDataExport.import(LabDataExport.export(emptyList(), listOf(rel), listOf(ctx)))

        assertEquals(rel, result.relationships.single())
        assertEquals(ctx, result.contexts.single())
    }
}
