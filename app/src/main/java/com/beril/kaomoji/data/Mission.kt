package com.beril.kaomoji.data

/**
 * Today's Tiny Mission.
 *
 * Not random. Not a timer. It looks at the whole academic state and picks the
 * single most meaningful next action, then explains WHY it picked it.
 */
data class Mission(
    val title: String,
    val why: String,
    val emoji: String,
    val kind: String,
    val taskId: String? = null,
    val unitId: String? = null,
    val recordingPrompt: String? = null,
    val mistakeId: String? = null,
    val projectId: String? = null,
    val assessmentId: String? = null
)

object MissionEngine {

    fun pick(store: Store): Mission {
        val c = store.curriculum
        val unit = store.currentUnit ?: return Mission(
            "Müfredat tamamlandı",
            "Dört ay bitti. Envanterini çıkar ve sıradaki planı yaz.",
            "🌳", "produce"
        )
        val phase = c.phaseOf(unit.id)

        val candidates = mutableListOf<Pair<Int, Mission>>()

        // ── 1. Assessment attached to the current unit, not yet taken ──
        c.assessments.firstOrNull { it.unitId == unit.id }?.let { a ->
            val st = store.assessmentStates[a.id]
            if (st?.taken != true) {
                val remaining = unit.tasks.count {
                    store.done[it.id] != true && store.skipped[it.id] != true && it.kind != "test"
                }
                if (remaining <= 2) {
                    candidates += 100 to Mission(
                        "${a.name} sınavına gir — ${a.hours} saat, kapalı kitap",
                        "Bu birimin geri kalanı bitti. Sınav şu an anlamlı: taze bilgiyi test etmek, unutmuş bilgiyi test etmekten çok daha öğretici.",
                        "📋", "test", assessmentId = a.id, unitId = unit.id
                    )
                }
            }
        }

        // ── 2. Feynman recording owed for a finished unit ──
        val prevIdx = c.unitIndexOf(unit.id) - 1
        val prevUnit = if (prevIdx >= 0) c.unitAt(prevIdx) else null
        if (prevUnit?.feynman != null &&
            store.recordings.none { it.unitId == prevUnit.id && it.isFeynman }
        ) {
            candidates += 92 to Mission(
                "Anlat ve kaydet: ${prevUnit.title}",
                "Bir önceki birimi bitirdin ama Feynman kaydını almadın. Kitaba bakmadan anlatamadığın yer, henüz öğrenmediğin yerdir.",
                "🎙️", "explain",
                unitId = prevUnit.id,
                recordingPrompt = prevUnit.feynman
            )
        }

        // ── 3. Unresolved mistakes piling up ──
        val openMistakes = store.mistakes.filter { !it.resolved }
        if (openMistakes.size >= 5) {
            val m = openMistakes.last()
            candidates += 88 to Mission(
                "Hata Defteri'nden ${openMistakes.size} soruyu yeniden çöz",
                "Hatalar birikti. En yüksek getirili tek iş, çözülmemiş hataları tekrar çözmek — yeni konu okumaktan daha değerli.",
                "🍂", "review", mistakeId = m.id
            )
        }
        val patterns = store.mistakePatterns()
        if (patterns.isNotEmpty() && openMistakes.size in 3..4) {
            val (subj, n) = patterns.first()
            val sname = c.subject(subj)?.name ?: subj
            candidates += 76 to Mission(
                "$sname konusundaki $n hatanı gözden geçir",
                "Aynı alanda tekrar eden hatalar var. Bu bir yetenek meselesi değil, kapanmamış bir kavram boşluğu.",
                "🔎", "review"
            )
        }

        // ── 4. Current unit's next incomplete task ──
        val nextTask = unit.tasks.firstOrNull {
            store.done[it.id] != true && store.skipped[it.id] != true
        }
        if (nextTask != null) {
            val kd = c.kind(nextTask.kind)
            val sd = c.subject(nextTask.subject)
            val base = when (nextTask.kind) {
                "problem" -> 70
                "code" -> 66
                "explain" -> 68
                "produce" -> 64
                "test" -> 74
                "write" -> 60
                else -> 58
            }
            val why = buildString {
                append(phase?.name ?: "")
                append(" · ")
                append(unit.title)
                append(" — ")
                append(
                    when (nextTask.kind) {
                        "problem" -> "Okumak kolaydır, çözmek öğretir. Sıradaki gerçek iş bu."
                        "code" -> "Bir denklemi kodlayabiliyorsan anlamışsındır. Kod yalan söylemez."
                        "explain" -> "Anlatmak, öğrenmenin son adımı."
                        "produce" -> "Portfolyonda görünen tek şey ürettiklerin."
                        "test" -> "Ölçmeden ilerlemek, karanlıkta yürümektir."
                        "review" -> "Tekrar olmadan öğrenme kalıcı olmaz."
                        else -> "Birimin sırası burada. Adım adım."
                    }
                )
            }
            candidates += base to Mission(
                nextTask.text,
                why,
                sd?.emoji ?: kd?.emoji ?: "🌱",
                nextTask.kind,
                taskId = nextTask.id,
                unitId = unit.id
            )
        }

        // ── 5. A project has gone quiet ──
        val phaseId = phase?.id ?: "p1"
        c.projects.forEach { p ->
            val st = store.projectStates[p.id]
            val hasWork = p.phaseWork.containsKey(phaseId)
            val recentlyTouched = store.recordings.any { it.projectId == p.id }
            if (hasWork && st != null && st.nextAction.isNotBlank() && !recentlyTouched) {
                val score = if (p.id == "P1" || p.id == "P3") 56 else 50
                candidates += score to Mission(
                    "${p.name}: ${st.nextAction}",
                    "Üretim blokları en kolay ertelenen, en zor telafi edilen kısım. ${p.emoji} bu fazda beklemede.",
                    p.emoji, "produce", projectId = p.id
                )
            }
        }

        // ── 6. Inbox overflowing ──
        val openInbox = store.inbox.count { !it.done }
        if (openInbox >= 8) {
            candidates += 48 to Mission(
                "Brain Inbox'ta $openInbox not birikti — ayıkla",
                "Yakalamak kolay, ayıklamak gerekli. Beş dakika ayır, gerçekten yapılacakları işaretle.",
                "🧺", "review"
            )
        }

        // ── 7. Weekly review overdue ──
        val weekMs = 7L * 24 * 3600 * 1000
        val lastReview = store.reviews.firstOrNull()?.createdAt ?: 0L
        if (System.currentTimeMillis() - lastReview > weekMs && store.totalDone > 10) {
            candidates += 52 to Mission(
                "Haftalık değerlendirmeyi yap",
                "Dört soru, beş dakika: ne ürettim, neyi kitapsız anlatabiliyorum, neye hâlâ bakıyorum, metrikler düşüyor mu?",
                "📝", "review"
            )
        }

        val best = candidates.maxByOrNull { it.first }?.second
        return best ?: Mission(
            "Bir sonraki birime geç: ${unit.title}",
            unit.kicker,
            "🌿", "study", unitId = unit.id
        )
    }

    /** A few alternatives, so the mission never feels like an order. */
    fun alternatives(store: Store, exclude: Mission): List<Mission> {
        val c = store.curriculum
        val unit = store.currentUnit ?: return emptyList()
        val out = mutableListOf<Mission>()

        unit.tasks.filter {
            store.done[it.id] != true && store.skipped[it.id] != true && it.id != exclude.taskId
        }.take(3).forEach { t ->
            out += Mission(
                t.text, "${c.subject(t.subject)?.name ?: ""} · ${t.minutes} dk",
                c.subject(t.subject)?.emoji ?: "🌱", t.kind, taskId = t.id, unitId = unit.id
            )
        }

        if (exclude.kind != "explain") {
            unit.feynman?.let {
                out += Mission(
                    "Bu birimi İngilizce anlat ve kaydet", "Feynman kuralı",
                    "🎙️", "explain", unitId = unit.id, recordingPrompt = it
                )
            }
        }
        return out.take(4)
    }
}
