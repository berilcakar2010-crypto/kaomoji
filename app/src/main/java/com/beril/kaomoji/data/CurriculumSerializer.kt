package com.beril.kaomoji.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * [Curriculum] nesnesini, [CurriculumLoader.parse]'ın beklediği kısa alan adı
 * şemasına birebir uyumlu JSON'a geri çevirir. Manuel müfredat düzenleyicisi
 * (CurriculumEditScreen), kullanıcı bir görevi/birimi değiştirdikten sonra bu
 * fonksiyonla üretilen JSON'u [CurriculumLoader.saveCustom] ile kaydeder.
 */
fun Curriculum.toJson(): String {
    val root = JSONObject()
    root.put("title", title)

    root.put("phases", JSONArray().also { arr ->
        phases.forEach { p ->
            arr.put(JSONObject().apply {
                put("id", p.id); put("name", p.name); put("sub", p.sub)
                put("goal", p.goal); put("hours", p.hours)
                put("units", JSONArray().also { uarr ->
                    p.units.forEach { u ->
                        uarr.put(JSONObject().apply {
                            put("id", u.id); put("title", u.title); put("kicker", u.kicker)
                            if (u.feynman != null) put("fey", u.feynman)
                            if (u.note != null) put("note", u.note)
                            put("br", JSONArray(u.bridges))
                            put("tasks", JSONArray().also { tarr ->
                                u.tasks.forEach { t ->
                                    tarr.put(JSONObject().apply {
                                        put("i", t.id); put("t", t.text); put("s", t.subject)
                                        put("k", t.kind); put("m", t.minutes)
                                        put("h", JSONArray(t.how))
                                        if (t.deliverable != null) put("o", t.deliverable)
                                    })
                                }
                            })
                        })
                    }
                })
            })
        }
    })

    root.put("projects", JSONArray().also { arr ->
        projects.forEach { p ->
            arr.put(JSONObject().apply {
                put("id", p.id); put("name", p.name); put("emoji", p.emoji); put("goal", p.goal)
                put("phases", JSONObject().also { o -> p.phaseWork.forEach { (k, v) -> o.put(k, v) } })
                put("topics", JSONArray(p.topics)); put("next", p.defaultNext)
                put("ms", JSONArray(p.milestones))
            })
        }
    })

    root.put("assessments", JSONArray().also { arr ->
        assessments.forEach { a ->
            arr.put(JSONObject().apply {
                put("id", a.id); put("name", a.name); put("scope", a.scope)
                put("hours", a.hours); put("phase", a.phaseId); put("unit", a.unitId)
            })
        }
    })

    root.put("bridges", JSONArray().also { arr ->
        bridges.forEach { b ->
            arr.put(JSONObject().apply {
                put("n", b.name); put("e", b.emoji); put("d", b.desc); put("t", JSONArray(b.topics))
            })
        }
    })

    root.put("resources", JSONArray().also { arr ->
        resources.forEach { r -> arr.put(JSONObject().apply { put("s", r.subject); put("n", r.name); put("u", r.use) }) }
    })

    root.put("subjects", JSONArray().also { arr ->
        subjects.forEach { s ->
            arr.put(JSONObject().apply { put("c", s.code); put("n", s.name); put("e", s.emoji); put("col", s.color) })
        }
    })

    root.put("kinds", JSONArray().also { arr ->
        kinds.forEach { k -> arr.put(JSONObject().apply { put("c", k.code); put("n", k.name); put("e", k.emoji) }) }
    })

    return root.toString()
}

/** Bir birim, verilen dönüşüm uygulanmış haliyle değiştirilerek yeni bir [Curriculum] döner. */
fun Curriculum.withUnitReplaced(unitId: String, transform: (CurriculumUnit) -> CurriculumUnit): Curriculum =
    copy(phases = phases.map { p -> p.copy(units = p.units.map { u -> if (u.id == unitId) transform(u) else u }) })

/** Yeni bir görev ekler, listeye ekler ve güncellenmiş müfredatı döner. */
fun Curriculum.withTaskAdded(unitId: String, task: Task): Curriculum =
    withUnitReplaced(unitId) { it.copy(tasks = it.tasks + task) }

/** Bir görevi (aynı id) günceller. */
fun Curriculum.withTaskUpdated(unitId: String, task: Task): Curriculum =
    withUnitReplaced(unitId) { u -> u.copy(tasks = u.tasks.map { if (it.id == task.id) task else it }) }

/** Bir görevi siler. */
fun Curriculum.withTaskRemoved(unitId: String, taskId: String): Curriculum =
    withUnitReplaced(unitId) { u -> u.copy(tasks = u.tasks.filterNot { it.id == taskId }) }

/** Bir birimin başlık/kicker/not alanlarını günceller. */
fun Curriculum.withUnitMetaUpdated(unitId: String, title: String, kicker: String, note: String?): Curriculum =
    withUnitReplaced(unitId) { it.copy(title = title, kicker = kicker, note = note) }
