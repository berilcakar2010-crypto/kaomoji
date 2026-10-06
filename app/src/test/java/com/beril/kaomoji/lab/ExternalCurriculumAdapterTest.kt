package com.beril.kaomoji.lab

import com.beril.kaomoji.lab.curriculum.ExternalCurriculumAdapter
import com.beril.kaomoji.lab.model.ContextKind
import com.beril.kaomoji.lab.model.ObjectKind
import com.beril.kaomoji.lab.model.RelationshipType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Küçük, gerçek pakettin şemasını birebir taklit eden bir örnek — tüm 469KB'ı değil, sadece
 *  şekli. Gerçek dosya app/src/main/assets/lab2_curriculum.json'da duruyor ve çalışma zamanında
 *  (LabHomeScreen → "İçe Aktar") bu aynı kod yoluyla işleniyor. */
private val SAMPLE = """
    {
      "manifest": {"curriculumId": "test"},
      "taxonomy": {},
      "hierarchy": [
        {"id": "dom.math", "level": "domain", "title": {"en": "Mathematics", "tr": "Matematik"}},
        {"id": "course.math-foundations", "level": "course", "title": {"en": "Foundations", "tr": "Temeller"}, "parentId": "dom.math"},
        {"id": "unit.math-proof", "level": "unit", "title": {"en": "Proof", "tr": "İspat"}, "parentId": "course.math-foundations"}
      ],
      "objects": [
        {
          "id": "math.found.proof-techniques",
          "kind": "skill",
          "title": {"en": "Proof techniques", "tr": "İspat teknikleri"},
          "description": "Direct proof, induction, contradiction.",
          "domainId": "dom.math",
          "parentId": "unit.math-proof",
          "contexts": ["school", "olympiad"],
          "prerequisites": []
        },
        {
          "id": "math.calc.derivative-as-rate",
          "kind": "concept",
          "title": {"en": "The derivative", "tr": "Türev"},
          "description": "Instantaneous rate of change.",
          "domainId": "dom.math",
          "parentId": "unit.math-proof",
          "contexts": ["ap"],
          "prerequisites": [{"id": "math.found.proof-techniques", "type": "soft"}]
        }
      ],
      "connections": [
        {"id": "conn.x", "from": "math.calc.derivative-as-rate", "to": "math.found.proof-techniques", "type": "formalizes", "why": "test why"}
      ],
      "contextMappings": [
        {
          "id": "ctx.school-math-general",
          "context": "school",
          "title": {"en": "General school math", "tr": "Genel lise matematiği"},
          "targets": ["math.found.proof-techniques"]
        }
      ]
    }
""".trimIndent()

class ExternalCurriculumAdapterTest {

    @Test
    fun `hierarchy nodes map to course or unit kind by level, with USED_IN to parent`() = runTest {
        val dao = FakeLabDao()
        ExternalCurriculumAdapter.import(SAMPLE, dao)

        assertEquals(ObjectKind.COURSE, dao.objects["dom.math"]?.kind)
        assertEquals(ObjectKind.COURSE, dao.objects["course.math-foundations"]?.kind)
        assertEquals(ObjectKind.CURRICULUM_UNIT, dao.objects["unit.math-proof"]?.kind)
        assertEquals("Matematik", dao.objects["dom.math"]?.title) // Türkçe başlık tercih edilir

        val rel = dao.relationships.values.find { it.fromId == "course.math-foundations" && it.toId == "dom.math" }
        assertNotNull(rel)
        assertEquals(RelationshipType.USED_IN, rel!!.type)
    }

    @Test
    fun `learning objects become CONCEPT kind with the external kind preserved in payload`() = runTest {
        val dao = FakeLabDao()
        ExternalCurriculumAdapter.import(SAMPLE, dao)

        val obj = dao.objects["math.found.proof-techniques"]
        assertEquals(ObjectKind.CONCEPT, obj?.kind)
        assertEquals("İspat teknikleri", obj?.title)
        assertTrue("payload should preserve the original external kind", obj!!.payload.contains("\"skill\""))
    }

    @Test
    fun `prerequisite type (soft, hard, tool, intuition) is preserved in the relationship note`() = runTest {
        val dao = FakeLabDao()
        ExternalCurriculumAdapter.import(SAMPLE, dao)

        val rel = dao.relationships.values.find {
            it.fromId == "math.found.proof-techniques" && it.toId == "math.calc.derivative-as-rate"
        }
        assertNotNull(rel)
        assertEquals(RelationshipType.PREREQUISITE_OF, rel!!.type)
        assertEquals("soft", rel.note)
    }

    @Test
    fun `cross-discipline connections become RELATED_TO with type and why preserved`() = runTest {
        val dao = FakeLabDao()
        ExternalCurriculumAdapter.import(SAMPLE, dao)

        val rel = dao.relationships.values.find {
            it.fromId == "math.calc.derivative-as-rate" && it.toId == "math.found.proof-techniques" && it.type == RelationshipType.RELATED_TO
        }
        assertNotNull(rel)
        assertTrue(rel!!.note!!.contains("formalizes"))
        assertTrue(rel.note!!.contains("test why"))
    }

    @Test
    fun `broad context tags and named context mappings both become Context rows and both reach the object`() = runTest {
        val dao = FakeLabDao()
        ExternalCurriculumAdapter.import(SAMPLE, dao)

        assertEquals(ContextKind.SCHOOL, dao.contexts["school"]?.kind)
        assertEquals(ContextKind.SCHOOL, dao.contexts["ctx.school-math-general"]?.kind)

        val obj = dao.objects["math.found.proof-techniques"]!!
        assertTrue(obj.contextIds.contains("school"))
        assertTrue(obj.contextIds.contains("olympiad"))
        assertTrue("named context mapping should also be added", obj.contextIds.contains("ctx.school-math-general"))
    }

    @Test
    fun `re-importing does not duplicate objects (tagged by sourcePackageId, delete-then-reinsert)`() = runTest {
        val dao = FakeLabDao()
        ExternalCurriculumAdapter.import(SAMPLE, dao)
        val firstCount = dao.objects.size

        ExternalCurriculumAdapter.import(SAMPLE, dao)

        assertEquals(firstCount, dao.objects.size)
    }
}
