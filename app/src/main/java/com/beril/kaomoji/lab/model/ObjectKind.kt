package com.beril.kaomoji.lab.model

/**
 * Lab 2.0 domain model — tek bir temel şekil (KnowledgeObjectEntity), bu discriminant ile
 * uzmanlaşır. Spec'in listelediği her "nesne tipi" (Concept, Question, Mistake, Project, Exam…)
 * ayrı bir tablo değil, burada bir değer — böylece ilişki motoru hepsine aynı anda hizmet eder
 * ve yeni bir tür (el yazısı not, kaynak/citation, embedding) yeni bir migration gerektirmez.
 */
enum class ObjectKind {
    CONCEPT,
    QUESTION,
    SKILL,
    PRACTICE,
    DERIVATION,
    EXPLANATION,
    FLASHCARD,
    MISTAKE,
    RESOURCE,
    NOTE,
    IDEA,
    PROJECT,
    EXPERIMENT,
    EXAM,
    ASSIGNMENT,
    COURSE,
    CURRICULUM_UNIT,
    LEARNING_SESSION,
    OUTPUT,
}

/** Bilgi nesnesinin ait olduğu akademik bağlam (okul, AP, olimpiyat, araştırma, proje…). */
enum class ContextKind {
    SCHOOL,
    AP,
    OLYMPIAD,
    INDEPENDENT_STUDY,
    RESEARCH,
    PROJECT,
    EXAM_PREP,
    OTHER,
}

/** İlişkiler birinci sınıf veri — grafik motoru bunları gerçek sorgularla okur, süs değil. */
enum class RelationshipType {
    PREREQUISITE_OF,
    USED_IN,
    TESTS,
    CAUSED_BY,
    EXPLAINS,
    REINFORCES,
    ASSESSES,
    RELATED_TO,
}

/**
 * Tarih/zamanlama her zaman opsiyonel bir yaprak alan — asla zorunlu omurga değil (§2/§18).
 * Bir suggestedDate'in geçmesi hiçbir şeyi "bozuk" yapmaz; sadece bir sorgu sonucudur.
 */
enum class ScheduleStatus {
    SUGGESTED,
    TARGET_SET,
    SCHEDULED,
    OVERDUE,
    COMPLETED,
    SKIPPED,
    PAUSED,
}
