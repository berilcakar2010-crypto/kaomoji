package com.beril.kaomoji.lab

import com.beril.kaomoji.lab.curriculum.CurriculumContractException
import com.beril.kaomoji.lab.curriculum.CurriculumContractParser
import com.beril.kaomoji.lab.curriculum.CurriculumImporter
import com.beril.kaomoji.lab.curriculum.LegacyCurriculumAdapter
import com.beril.kaomoji.lab.model.ObjectKind
import com.beril.kaomoji.lab.model.RelationshipType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CurriculumImportTest {

    private val minimalContractJson = """
        {
          "packageId": "pkg-1",
          "contractVersion": "1.0",
          "title": "Test Paketi",
          "domains": [{"id":"d1","name":"Fizik"}],
          "courses": [{"id":"c1","domainId":"d1","name":"Elektromanyetizma","contexts":["AP"]}],
          "units": [{"id":"u1","courseId":"c1","title":"İndüksiyon","objectives":["Faraday yasasını anla"],"prerequisiteUnitIds":[]}],
          "concepts": [{"id":"k1","unitId":"u1","title":"Faraday Yasası","body":"EMF = -dΦ/dt"}],
          "questions": [{"id":"q1","conceptIds":["k1"],"prompt":"Akı değişirse ne olur?","kind":"PREDICT"}],
          "assessments": [{"id":"e1","kind":"EXAM","scopeConceptIds":["k1"]}],
          "relationships": [{"fromId":"q1","toId":"k1","type":"TESTS"}],
          "resources": []
        }
    """.trimIndent()

    @Test
    fun `parser rejects wrong contract version`() {
        val bad = minimalContractJson.replace("\"1.0\"", "\"0.9\"")
        assertThrows(CurriculumContractException::class.java) { CurriculumContractParser.parse(bad) }
    }

    @Test
    fun `parser rejects invalid json`() {
        assertThrows(CurriculumContractException::class.java) { CurriculumContractParser.parse("not json") }
    }

    @Test
    fun `parser reads a minimal valid package`() {
        val pkg = CurriculumContractParser.parse(minimalContractJson)
        assertEquals("pkg-1", pkg.packageId)
        assertEquals(1, pkg.courses.size)
        assertEquals(1, pkg.concepts.size)
        assertEquals("EXAM", pkg.assessments.first().kind)
    }

    @Test
    fun `importer writes objects and relationships and tags them with sourcePackageId`() = runTest {
        val dao = FakeLabDao()
        val importer = CurriculumImporter(dao)
        val pkg = CurriculumContractParser.parse(minimalContractJson)

        importer.import(pkg)

        assertEquals(ObjectKind.COURSE, dao.objects["c1"]?.kind)
        assertEquals(ObjectKind.CURRICULUM_UNIT, dao.objects["u1"]?.kind)
        assertEquals(ObjectKind.CONCEPT, dao.objects["k1"]?.kind)
        assertEquals(ObjectKind.QUESTION, dao.objects["q1"]?.kind)
        assertEquals(ObjectKind.EXAM, dao.objects["e1"]?.kind)
        dao.objects.values.forEach { assertEquals("pkg-1", it.sourcePackageId) }

        val testsRel = dao.relationships.values.find { it.type == RelationshipType.TESTS && it.fromId == "q1" }
        assertNotNull(testsRel)
        assertEquals("k1", testsRel!!.toId)

        // concept -> unit ilişkisi otomatik üretilmiş olmalı
        assertTrue(dao.relationships.values.any { it.fromId == "k1" && it.toId == "u1" && it.type == RelationshipType.USED_IN })
    }

    @Test
    fun `re-importing the same package replaces old rows instead of accumulating them`() = runTest {
        val dao = FakeLabDao()
        val importer = CurriculumImporter(dao)
        val pkg = CurriculumContractParser.parse(minimalContractJson)

        importer.import(pkg)
        val firstCount = dao.objects.size
        importer.import(pkg)

        assertEquals(firstCount, dao.objects.size)
    }

    private val legacyJson = """
        {
          "subjects": [{"c":"p1","n":"P1","e":"🧠","col":"#000"}],
          "kinds": [{"c":"study","n":"Öğren","e":"📖"}],
          "phases": [{
            "id": "faz1", "name": "Faz 1", "sub": "sub", "goal": "goal", "hours": 1,
            "units": [{
              "id": "u1", "title": "Hafta 1", "kicker": "1-7 Eylül",
              "tasks": [{"i":"t1","t":"20 Eylül · İlk görev","s":"p1","k":"study","m":30}]
            }]
          }],
          "projects": [], "bridges": []
        }
    """.trimIndent()

    @Test
    fun `legacy adapter converts old curriculum json into a valid contract package`() = runTest {
        val pkg = LegacyCurriculumAdapter.toContractPackage(legacyJson)
        assertEquals("1.0", pkg.contractVersion)
        assertEquals(1, pkg.courses.size)
        assertEquals(1, pkg.units.size)
        assertEquals(1, pkg.concepts.size)
        assertEquals(1, pkg.questions.size)
        assertEquals("20 Eylül · İlk görev", pkg.questions.first().prompt)

        val dao = FakeLabDao()
        CurriculumImporter(dao).import(pkg)
        assertEquals(ObjectKind.QUESTION, dao.objects["t1"]?.kind)
    }
}
