package com.beril.kaomoji.lab.graph

import com.beril.kaomoji.lab.model.RelationshipEntity
import com.beril.kaomoji.lab.model.RelationshipType
import org.junit.Assert.assertEquals
import org.junit.Test

class ConceptRelationshipsTest {

    @Test
    fun `an edge pointing INTO the focal concept is a prerequisite of it`() {
        val rels = listOf(RelationshipEntity(fromId = "A", toId = "focus", type = RelationshipType.PREREQUISITE_OF, note = "hard"))
        val buckets = bucketRelationships("focus", rels)
        assertEquals(listOf(BucketedEdge("A", "hard", EdgeBucket.PREREQUISITE)), buckets)
    }

    @Test
    fun `an edge pointing OUT of the focal concept means the focal concept enables it`() {
        val rels = listOf(RelationshipEntity(fromId = "focus", toId = "B", type = RelationshipType.PREREQUISITE_OF, note = "soft"))
        val buckets = bucketRelationships("focus", rels)
        assertEquals(listOf(BucketedEdge("B", "soft", EdgeBucket.ENABLES)), buckets)
    }

    @Test
    fun `a RELATED_TO edge resolves to whichever side is NOT the focal concept, regardless of direction`() {
        val outgoing = listOf(RelationshipEntity(fromId = "focus", toId = "C", type = RelationshipType.RELATED_TO, note = "formalizes: x"))
        val incoming = listOf(RelationshipEntity(fromId = "C", toId = "focus", type = RelationshipType.RELATED_TO, note = "formalizes: x"))

        assertEquals(listOf(BucketedEdge("C", "formalizes: x", EdgeBucket.RELATED)), bucketRelationships("focus", outgoing))
        assertEquals(listOf(BucketedEdge("C", "formalizes: x", EdgeBucket.RELATED)), bucketRelationships("focus", incoming))
    }

    @Test
    fun `edges unrelated to the focal concept's type vocabulary are ignored, not mis-bucketed`() {
        val rels = listOf(RelationshipEntity(fromId = "focus", toId = "D", type = RelationshipType.TESTS))
        assertEquals(emptyList<BucketedEdge>(), bucketRelationships("focus", rels))
    }
}
