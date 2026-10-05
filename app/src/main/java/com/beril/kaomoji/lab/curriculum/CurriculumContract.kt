package com.beril.kaomoji.lab.curriculum

/**
 * Curriculum Contract v1 (spec §17/§52).
 *
 * Bu uygulama müfredat İÇERİĞİNİ üretmez — hangi konuların ne zaman çalışılacağına karar
 * vermez, fizik/AP/nörobilim içeriğini kodlamaz. Bu dosya sadece SÖZLEŞMEYİ (şekli) tanımlar:
 * ayrı bir müfredat-üretim projesi bu şekle uyan JSON üretir, bu uygulama da iç yapısını
 * bilmeden bunu içe aktarır. Müfredat tamamen değiştirilse (farklı proje, farklı yıl, farklı
 * alan) bile uygulama çalışmaya devam eder — çünkü hiçbir ekran bu içeriğe hardcode bağlı değil.
 *
 * contractVersion bu sürümle eşleşmiyorsa `CurriculumImporter` içe aktarmayı reddeder ve
 * kullanıcıya okunabilir bir hata verir (§48) — sessizce yanlış yorumlamaz.
 */
object CurriculumContractVersion {
    const val CURRENT = "1.0"
}

data class CurriculumPackage(
    val packageId: String,
    val contractVersion: String,
    val title: String,
    val domains: List<Domain>,
    val courses: List<Course>,
    val units: List<Unit_>,
    val concepts: List<Concept>,
    val questions: List<Question>,
    val assessments: List<Assessment>,
    val relationships: List<RelationshipDef>,
    val resources: List<ResourceDef>,
)

data class Domain(val id: String, val name: String)

data class Course(val id: String, val domainId: String, val name: String, val contexts: List<String>)

data class Unit_(
    val id: String,
    val courseId: String,
    val title: String,
    val objectives: List<String> = emptyList(),
    val prerequisiteUnitIds: List<String> = emptyList(),
    /** Opsiyonel — "bu birim ~20 Eylül'de önerilir" gibi. Yok olması hiçbir şeyi bozmaz. */
    val suggestedDateEpochDay: Long? = null,
)

data class Concept(val id: String, val unitId: String, val title: String, val body: String? = null)

data class Question(
    val id: String,
    val conceptIds: List<String>,
    val prompt: String,
    /** PREDICT | SOLVE | DERIVE | EXPLAIN — §12-14'teki soru-önce öğrenme akışına karşılık gelir. */
    val kind: String,
    val answer: String? = null,
)

data class Assessment(
    val id: String,
    /** EXAM | ASSIGNMENT */
    val kind: String,
    val scopeConceptIds: List<String> = emptyList(),
    val suggestedDateEpochDay: Long? = null,
)

data class RelationshipDef(val fromId: String, val toId: String, val type: String, val note: String? = null)

data class ResourceDef(val id: String, val title: String, val url: String? = null, val note: String? = null)
