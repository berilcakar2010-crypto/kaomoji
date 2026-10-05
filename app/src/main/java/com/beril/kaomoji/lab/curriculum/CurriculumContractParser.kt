package com.beril.kaomoji.lab.curriculum

import org.json.JSONArray
import org.json.JSONObject

class CurriculumContractException(message: String) : Exception(message)

/** Contract v1 JSON <-> [CurriculumPackage]. Hatalar fırlatıldığında çağıran taraf (§48)
 *  kullanıcıya "bu dosya içe aktarılamadı çünkü ..." diyebilsin diye okunabilir bir mesaj taşır. */
object CurriculumContractParser {

    fun parse(raw: String): CurriculumPackage {
        val root = try {
            JSONObject(raw)
        } catch (e: Exception) {
            throw CurriculumContractException("Bu dosya geçerli bir JSON değil.")
        }

        val version = root.optString("contractVersion", "")
        if (version != CurriculumContractVersion.CURRENT) {
            throw CurriculumContractException(
                "Bu müfredat paketi sürüm $version ile üretilmiş, uygulama ${CurriculumContractVersion.CURRENT} bekliyor."
            )
        }
        if (!root.has("packageId")) {
            throw CurriculumContractException("Müfredat paketinde packageId eksik.")
        }

        return CurriculumPackage(
            packageId = root.getString("packageId"),
            contractVersion = version,
            title = root.optString("title", "Müfredat"),
            domains = root.optJSONArray("domains").orEmpty().map {
                Domain(it.getString("id"), it.getString("name"))
            },
            courses = root.optJSONArray("courses").orEmpty().map {
                Course(it.getString("id"), it.getString("domainId"), it.getString("name"), it.optJSONArray("contexts").orEmpty().strings())
            },
            units = root.optJSONArray("units").orEmpty().map {
                Unit_(
                    id = it.getString("id"),
                    courseId = it.getString("courseId"),
                    title = it.getString("title"),
                    objectives = it.optJSONArray("objectives").orEmpty().strings(),
                    prerequisiteUnitIds = it.optJSONArray("prerequisiteUnitIds").orEmpty().strings(),
                    suggestedDateEpochDay = it.optLongOrNull("suggestedDateEpochDay"),
                )
            },
            concepts = root.optJSONArray("concepts").orEmpty().map {
                Concept(it.getString("id"), it.getString("unitId"), it.getString("title"), it.optStringOrNull("body"))
            },
            questions = root.optJSONArray("questions").orEmpty().map {
                Question(
                    id = it.getString("id"),
                    conceptIds = it.optJSONArray("conceptIds").orEmpty().strings(),
                    prompt = it.getString("prompt"),
                    kind = it.optString("kind", "SOLVE"),
                    answer = it.optStringOrNull("answer"),
                )
            },
            assessments = root.optJSONArray("assessments").orEmpty().map {
                Assessment(
                    id = it.getString("id"),
                    kind = it.optString("kind", "EXAM"),
                    scopeConceptIds = it.optJSONArray("scopeConceptIds").orEmpty().strings(),
                    suggestedDateEpochDay = it.optLongOrNull("suggestedDateEpochDay"),
                )
            },
            relationships = root.optJSONArray("relationships").orEmpty().map {
                RelationshipDef(it.getString("fromId"), it.getString("toId"), it.getString("type"), it.optStringOrNull("note"))
            },
            resources = root.optJSONArray("resources").orEmpty().map {
                ResourceDef(it.getString("id"), it.getString("title"), it.optStringOrNull("url"), it.optStringOrNull("note"))
            },
        )
    }

    private fun JSONArray?.orEmpty(): JSONArray = this ?: JSONArray()
    private inline fun <T> JSONArray.map(block: (JSONObject) -> T): List<T> = (0 until length()).map { block(getJSONObject(it)) }
    private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }
    private fun JSONObject.optStringOrNull(key: String): String? = if (has(key) && !isNull(key)) getString(key) else null
    private fun JSONObject.optLongOrNull(key: String): Long? = if (has(key) && !isNull(key)) getLong(key) else null
}
