package com.beril.kaomoji.lab.graph

import com.beril.kaomoji.lab.model.RelationshipEntity
import com.beril.kaomoji.lab.model.RelationshipType

/**
 * §16: "the graph must power actual application behavior", gösterişli bir çizim değil. Bu
 * saf fonksiyon bir odak kavram için gelen/giden ilişkileri üç anlamlı kovaya ayırır — UI
 * (ConceptGraphScreen) sadece bunu çağırıp listeler, yön/tür mantığını kendi içinde tekrar
 * etmez.
 */
enum class EdgeBucket { PREREQUISITE, ENABLES, RELATED }

data class BucketedEdge(val otherId: String, val note: String?, val bucket: EdgeBucket)

fun bucketRelationships(conceptId: String, rels: List<RelationshipEntity>): List<BucketedEdge> =
    rels.mapNotNull { r ->
        when {
            r.type == RelationshipType.PREREQUISITE_OF && r.toId == conceptId ->
                BucketedEdge(r.fromId, r.note, EdgeBucket.PREREQUISITE)
            r.type == RelationshipType.PREREQUISITE_OF && r.fromId == conceptId ->
                BucketedEdge(r.toId, r.note, EdgeBucket.ENABLES)
            r.type == RelationshipType.RELATED_TO ->
                BucketedEdge(if (r.fromId == conceptId) r.toId else r.fromId, r.note, EdgeBucket.RELATED)
            else -> null
        }
    }
