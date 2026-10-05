package com.beril.kaomoji.lab.curriculum

import org.json.JSONObject

/**
 * Köprü adaptörü: eski `assets/curriculum.json` şemasını (phases→units→tasks, kısa alan adları
 * s/k/t/m) Curriculum Contract v1 şekline çevirir, böylece eski içerik de aynı import yolundan
 * (`CurriculumImporter`) geçer — ayrı bir "legacy store" yazmaya gerek kalmaz.
 *
 * DÜRÜST SINIRLAMA: eski şemada birim başına tek bir ders yok (görevler birden fazla konuyu
 * karıştırabilir) ve ayrı bir sınav/ödev nesnesi yok. Bu adaptör bu yüzden:
 *  - her birim için TEK bir Concept üretir (birimin kendisi),
 *  - her görevi o Concept'e bağlı bir Question'a çevirir,
 *  - "bridges" (eski köprüler) listesini içe aktarmaz — çünkü serbest metin konu adlarına
 *    referans veriyorlar, gerçek nesne id'lerine değil; sahte bir ilişki üretmek yerine
 *    hiç üretmemek daha dürüst (§51).
 * Gerçek müfredat-üretim projesi contract v1'i doğrudan üretmeye başladığında bu dosya
 * silinecek — kalıcı bir mimari parça değil, tek seferlik bir göç köprüsü.
 */
object LegacyCurriculumAdapter {
    private const val LEGACY_DOMAIN_ID = "legacy"

    fun toContractPackage(raw: String, packageId: String = "legacy-curriculum"): CurriculumPackage {
        val root = JSONObject(raw)
        val subjects = root.getJSONArray("subjects")
        val subjectCodes = (0 until subjects.length()).map { subjects.getJSONObject(it).getString("c") }
        val fallbackSubject = subjectCodes.firstOrNull() ?: "genel"

        val courses = (0 until subjects.length()).map {
            val s = subjects.getJSONObject(it)
            Course(id = s.getString("c"), domainId = LEGACY_DOMAIN_ID, name = s.getString("n"), contexts = emptyList())
        }

        val units = mutableListOf<Unit_>()
        val concepts = mutableListOf<Concept>()
        val questions = mutableListOf<Question>()

        val phases = root.getJSONArray("phases")
        for (pi in 0 until phases.length()) {
            val phase = phases.getJSONObject(pi)
            val unitArr = phase.getJSONArray("units")
            for (ui in 0 until unitArr.length()) {
                val u = unitArr.getJSONObject(ui)
                val unitId = u.getString("id")
                val taskArr = u.getJSONArray("tasks")
                val taskSubjects = (0 until taskArr.length()).map { taskArr.getJSONObject(it).optString("s", fallbackSubject) }
                val courseId = taskSubjects.firstOrNull() ?: fallbackSubject

                units += Unit_(id = unitId, courseId = courseId, title = u.getString("title"))
                val conceptId = "concept-$unitId"
                concepts += Concept(id = conceptId, unitId = unitId, title = u.getString("title"), body = u.optString("kicker", null))

                for (ti in 0 until taskArr.length()) {
                    val t = taskArr.getJSONObject(ti)
                    questions += Question(
                        id = t.getString("i"),
                        conceptIds = listOf(conceptId),
                        prompt = t.getString("t"),
                        kind = legacyTaskKindToQuestionKind(t.optString("k", "study")),
                    )
                }
            }
        }

        return CurriculumPackage(
            packageId = packageId,
            contractVersion = CurriculumContractVersion.CURRENT,
            title = "Mevcut Müfredat (göç edilmiş)",
            domains = listOf(Domain(LEGACY_DOMAIN_ID, "Mevcut Müfredat")),
            courses = courses,
            units = units,
            concepts = concepts,
            questions = questions,
            assessments = emptyList(),
            relationships = emptyList(),
            resources = emptyList(),
        )
    }

    private fun legacyTaskKindToQuestionKind(k: String): String = when (k) {
        "produce" -> "SOLVE"
        "review" -> "SOLVE"
        "test" -> "SOLVE"
        "reflect", "explain" -> "EXPLAIN"
        else -> "EXPLAIN"
    }
}
