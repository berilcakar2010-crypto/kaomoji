package com.beril.kaomoji.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beril.kaomoji.data.*
import com.beril.kaomoji.storage.FileVault
import com.beril.kaomoji.storage.humanSize

// ══════════════════════════ BRAIN INBOX ══════════════════════════

@Composable
fun InboxScreen(store: Store) {
    var text by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("Fikir") }
    var showDone by remember { mutableStateOf(false) }

    val list = store.inbox.filter { showDone || !it.done }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, 12.dp, 14.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item {
            Text("🧺 Brain Inbox", style = Display)
            Text("Yakala şimdi, düzenle sonra.", style = Small)
        }
        item {
            Card(border = J.bark.copy(alpha = 0.35f)) {
                Field(text, { text = it }, "Aklında ne var?", minLines = 2)
                Spacer(Modifier.height(9.dp))
                Selector(Categories.inbox, cat, { cat = it })
                Spacer(Modifier.height(10.dp))
                Btn("Kaydet", {
                    if (text.isNotBlank()) { store.addInbox(text.trim(), cat); text = "" }
                }, bg = J.bark, emoji = "🧺")
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("notlar", "📌")
            }
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.clickable { showDone = !showDone },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(showDone, { showDone = !showDone }, J.inkSoft)
                Spacer(Modifier.width(8.dp))
                Text("Tamamlananları göster", style = Small)
            }
        }

        if (list.isEmpty()) item { Empty("🍃", "Inbox boş", "Temiz bir kafa iyi bir şey.") }

        items(list) { n ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(
                        if (n.done) J.paperDeep.copy(alpha = 0.5f) else J.card,
                        RoundedCornerShape(14.dp)
                    )
                    .border(1.dp, J.line, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Checkbox(n.done, { store.updateInbox(n.copy(done = !n.done)) }, J.bark)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Chip(n.category, J.bark)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        n.text,
                        style = if (n.done) Body.copy(color = J.inkFaint) else Body
                    )
                }
                Text(
                    "✕", style = TextStyle(fontSize = 14.sp, color = J.inkFaint),
                    modifier = Modifier.clickable { store.deleteInbox(n.id) }.padding(4.dp)
                )
            }
        }
    }
}

// ══════════════════════════ PROJECTS ══════════════════════════

@Composable
fun ProjectsScreen(store: Store, onOpen: (String) -> Unit) {
    val c = store.curriculum
    val phaseId = store.currentPhase?.id ?: "p1"

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, 12.dp, 14.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("🌱 Projeler", style = Display)
            Text("Dersler projelere hizmet eder, tersi değil.", style = Small)
        }
        items(c.projects) { p ->
            val st = store.projectStates[p.id]
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(J.card, RoundedCornerShape(18.dp))
                    .border(1.dp, J.line, RoundedCornerShape(18.dp))
                    .clickable { onOpen(p.id) }
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Sticker(p.emoji, 38, J.lime.copy(alpha = 0.3f))
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.name, style = TitleL)
                        Text(p.id, style = Tiny)
                    }
                }
                Spacer(Modifier.height(9.dp))
                Text(p.goal, style = Small.copy(color = J.ink))
                Spacer(Modifier.height(10.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(J.apple.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Text("SIRADAKİ EYLEM", style = Tiny.copy(color = J.forest, fontWeight = FontWeight.Bold))
                    Spacer(Modifier.height(3.dp))
                    Text(st?.nextAction?.ifBlank { p.defaultNext } ?: p.defaultNext, style = Body)
                }
                p.phaseWork[phaseId]?.let {
                    Spacer(Modifier.height(8.dp))
                    Text("Bu fazda: $it", style = Small)
                }
            }
        }
    }
}

@Composable
fun ProjectDetailScreen(store: Store, projectId: String, onBack: () -> Unit, onRecord: (RecordRequest) -> Unit) {
    val c = store.curriculum
    val p = c.projects.firstOrNull { it.id == projectId } ?: return
    val st = store.projectStates[p.id] ?: ProjectState()
    var next by remember { mutableStateOf(st.nextAction.ifBlank { p.defaultNext }) }
    var notes by remember { mutableStateOf(st.notes) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, 12.dp, 14.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        item {
            GhostBtn("Geri", onBack, emoji = "←")
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.emoji, style = TextStyle(fontSize = 30.sp))
                Spacer(Modifier.width(10.dp))
                Text(p.name, style = Display)
            }
            Spacer(Modifier.height(6.dp))
            Text(p.goal, style = Body)
        }

        item {
            SectionLabel("sıradaki eylem", "🎯")
            Spacer(Modifier.height(7.dp))
            Field(next, { next = it }, "Somut tek bir adım yaz", minLines = 2)
            Spacer(Modifier.height(8.dp))
            Btn("Güncelle", {
                store.setProject(p.id, st.copy(nextAction = next, notes = notes))
            }, bg = J.forest, emoji = "✓")
        }

        item {
            SectionLabel("faz planı", "📅")
            Spacer(Modifier.height(7.dp))
            c.phases.forEach { ph ->
                val work = p.phaseWork[ph.id] ?: return@forEach
                val isCurrent = ph.id == store.currentPhase?.id
                val ms = "${p.id}-${ph.id}"
                val done = st.milestonesDone.contains(ms)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 7.dp)
                        .background(
                            if (isCurrent) J.mint.copy(alpha = 0.18f) else J.card,
                            RoundedCornerShape(13.dp)
                        )
                        .border(1.dp, if (isCurrent) J.apple.copy(alpha = 0.4f) else J.lineSoft, RoundedCornerShape(13.dp))
                        .padding(11.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Checkbox(done, {
                        val l = st.milestonesDone.toMutableList()
                        if (done) l.remove(ms) else l.add(ms)
                        store.setProject(p.id, st.copy(milestonesDone = l))
                    })
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Row {
                            Text(ph.name, style = TitleM.copy(fontSize = 14.sp))
                            if (isCurrent) { Spacer(Modifier.width(6.dp)); Chip("şimdi", J.cherry) }
                        }
                        Text(work, style = Small.copy(color = J.ink))
                    }
                }
            }
        }

        item {
            SectionLabel("ilgili konular", "🔗")
            Spacer(Modifier.height(7.dp))
            Row(Modifier.fillMaxWidth()) {
                Column {
                    p.topics.forEach { Text("· $it", style = Body) }
                }
            }
        }

        item {
            SectionLabel("notlar", "📝")
            Spacer(Modifier.height(7.dp))
            Field(notes, { notes = it }, "Proje notların…", minLines = 4)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn("Notu kaydet", {
                    store.setProject(p.id, st.copy(nextAction = next, notes = notes))
                }, Modifier.weight(1f), J.forest)
                GhostBtn("Anlat", {
                    onRecord(RecordRequest("${p.name} projesinde ne yaptığını anlat", null, null, false, p.id))
                }, emoji = "🎙️")
            }
        }

        val recs = store.recordings.filter { it.projectId == p.id }
        if (recs.isNotEmpty()) {
            item { SectionLabel("proje anlatımları", "🎙️") }
            items(recs) { r ->
                Card(border = J.lilac.copy(alpha = 0.35f)) {
                    Text(r.title, style = Body, maxLines = 2)
                    Text(com.beril.kaomoji.audio.fmtDuration(r.durationMs), style = Tiny)
                }
            }
        }
    }
}

// ══════════════════════════ MISTAKES ══════════════════════════

@Composable
fun MistakesScreen(store: Store, presetUnit: String?, onBack: () -> Unit) {
    val c = store.curriculum
    var adding by remember { mutableStateOf(presetUnit != null) }
    var filter by remember { mutableStateOf("Açık") }

    val list = store.mistakes.filter {
        when (filter) {
            "Açık" -> !it.resolved
            "Çözüldü" -> it.resolved
            else -> true
        }
    }
    val patterns = store.mistakePatterns()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, 12.dp, 14.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            GhostBtn("Geri", onBack, emoji = "←")
            Spacer(Modifier.height(10.dp))
            Text("🍂 Hata Defteri", style = Display)
            Text("Hata bir başarısızlık değil, bir adres.", style = Small)
        }

        if (patterns.isNotEmpty()) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(J.butter.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                        .dashed(J.butter.copy(alpha = 0.6f), radius = 14f)
                        .padding(12.dp)
                ) {
                    Text("🔎 ÖRÜNTÜ", style = Tiny.copy(color = J.bark, fontWeight = FontWeight.Bold))
                    Spacer(Modifier.height(5.dp))
                    patterns.forEach { (s, n) ->
                        Text(
                            "${c.subject(s)?.name ?: s} alanında $n açık hata var — burada kapanmamış bir kavram olabilir.",
                            style = Small.copy(color = J.ink)
                        )
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn(if (adding) "Kapat" else "Hata ekle", { adding = !adding },
                    Modifier.weight(1f), J.butter, Color(0xFF3A2F16), if (adding) "✕" else "＋")
            }
        }

        if (adding) item { MistakeForm(store, presetUnit) { adding = false } }

        item { Selector(listOf("Açık", "Çözüldü", "Tümü"), filter, { filter = it }) }

        if (list.isEmpty()) item { Empty("🍃", "Hata yok", "Ya çok iyisin ya da kaydetmiyorsun.") }

        items(list) { m ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(J.card, RoundedCornerShape(16.dp))
                    .border(1.dp, if (m.resolved) J.lineSoft else J.butter.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                    .padding(13.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Chip(m.category, J.bark)
                    Spacer(Modifier.width(5.dp))
                    c.subject(m.subject)?.let { Chip("${it.emoji} ${it.name}", subjectColor(it.color)) }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "✕", style = TextStyle(fontSize = 14.sp, color = J.inkFaint),
                        modifier = Modifier.clickable { store.deleteMistake(m.id) }.padding(4.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("SORU", style = Tiny.copy(fontWeight = FontWeight.Bold))
                Text(m.problem, style = Body)
                if (m.why.isNotBlank()) {
                    Spacer(Modifier.height(7.dp))
                    Text("NEDEN YANLIŞ YAPTIM", style = Tiny.copy(color = J.cherry, fontWeight = FontWeight.Bold))
                    Text(m.why, style = Body)
                }
                if (m.correct.isNotBlank()) {
                    Spacer(Modifier.height(7.dp))
                    Text("DOĞRU YAKLAŞIM", style = Tiny.copy(color = J.forest, fontWeight = FontWeight.Bold))
                    Text(m.correct, style = Body)
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.clickable { store.updateMistake(m.copy(resolved = !m.resolved)) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(m.resolved, { store.updateMistake(m.copy(resolved = !m.resolved)) })
                    Spacer(Modifier.width(9.dp))
                    Text("Yeniden çözdüm, oturdu", style = Small)
                }
            }
        }
    }
}

@Composable
private fun MistakeForm(store: Store, presetUnit: String?, onDone: () -> Unit) {
    val c = store.curriculum
    var problem by remember { mutableStateOf("") }
    var why by remember { mutableStateOf("") }
    var correct by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf(Categories.mistake.first()) }
    var subj by remember { mutableStateOf("phys") }

    Card(border = J.butter.copy(alpha = 0.5f)) {
        Text("Soru", style = Small); Spacer(Modifier.height(4.dp))
        Field(problem, { problem = it }, "Hangi soru?", minLines = 2)
        Spacer(Modifier.height(9.dp))
        Text("Neden yanlış yaptım", style = Small); Spacer(Modifier.height(4.dp))
        Field(why, { why = it }, "Dürüst ol — asıl değer burada", minLines = 2)
        Spacer(Modifier.height(9.dp))
        Text("Doğru yaklaşım", style = Small); Spacer(Modifier.height(4.dp))
        Field(correct, { correct = it }, "Bir dahaki sefere nasıl?", minLines = 2)
        Spacer(Modifier.height(9.dp))
        Text("Kategori", style = Small); Spacer(Modifier.height(4.dp))
        Selector(Categories.mistake, cat, { cat = it })
        Spacer(Modifier.height(9.dp))
        Text("Ders", style = Small); Spacer(Modifier.height(4.dp))
        Selector(c.subjects.map { it.code }, subj, { subj = it },
            labels = { code -> c.subject(code)?.let { "${it.emoji} ${it.name}" } ?: code })
        Spacer(Modifier.height(12.dp))
        Btn("Deftere ekle", {
            if (problem.isNotBlank()) {
                store.addMistake(
                    Mistake(
                        uid(), problem.trim(), why.trim(), correct.trim(),
                        cat, subj, presetUnit, System.currentTimeMillis()
                    )
                )
                onDone()
            }
        }, bg = J.butter, fg = Color(0xFF3A2F16), emoji = "🍂")
    }
}

// ══════════════════════════ PROBLEM LOG ══════════════════════════

@Composable
fun ProblemLogDialog(store: Store, unitId: String?, onClose: () -> Unit) {
    val c = store.curriculum
    var attempted by remember { mutableStateOf("") }
    var solved by remember { mutableStateOf("") }
    var subj by remember { mutableStateOf("phys") }

    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        Sheet("✏️ Problem kaydı", onClose) {
            Text(
                "Kaç saat oturduğun değil, kaç problem çözdüğün önemli.",
                style = Small
            )
            Spacer(Modifier.height(11.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Denenen", style = Small)
                    Spacer(Modifier.height(4.dp))
                    Field(attempted, { attempted = it.filter(Char::isDigit) }, "0", single = true)
                }
                Column(Modifier.weight(1f)) {
                    Text("Doğru", style = Small)
                    Spacer(Modifier.height(4.dp))
                    Field(solved, { solved = it.filter(Char::isDigit) }, "0", single = true)
                }
            }
            Spacer(Modifier.height(10.dp))
            Selector(c.subjects.map { it.code }, subj, { subj = it },
                labels = { code -> c.subject(code)?.let { "${it.emoji} ${it.name}" } ?: code })
            Spacer(Modifier.height(13.dp))
            Btn("Kaydet", {
                store.logProblems(unitId, subj, attempted.toIntOrNull() ?: 0, solved.toIntOrNull() ?: 0)
                onClose()
            }, bg = J.cherry, emoji = "✏️")
        }
    }
}

// ══════════════════════════ ASSESSMENTS ══════════════════════════

@Composable
fun AssessmentsScreen(store: Store, onBack: () -> Unit) {
    val c = store.curriculum
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, 12.dp, 14.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            GhostBtn("Geri", onBack, emoji = "←")
            Spacer(Modifier.height(10.dp))
            Text("📋 Sınavlar", style = Display)
            Text("Geri sayım yok. Hazır olduğunda gir.", style = Small)
        }
        items(c.assessments) { a ->
            val st = store.assessmentStates[a.id] ?: AssessmentState()
            val unit = c.allUnits.firstOrNull { it.id == a.unitId }
            val reached = unit != null && c.unitIndexOf(unit.id) <= store.currentUnitIndex
            var score by remember(a.id) { mutableStateOf(st.score) }

            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        if (st.taken) J.mint.copy(alpha = 0.16f) else J.card,
                        RoundedCornerShape(16.dp)
                    )
                    .border(
                        1.dp,
                        if (reached && !st.taken) J.cherry.copy(alpha = 0.45f) else J.line,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(13.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (st.taken) "🍎" else "📋", style = TextStyle(fontSize = 20.sp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(a.name, style = TitleL.copy(fontSize = 17.sp))
                        Text("${a.hours} saat · ${c.phases.firstOrNull { it.id == a.phaseId }?.name ?: ""}", style = Tiny)
                    }
                    if (reached && !st.taken) Chip("hazır", J.cherry)
                }
                Spacer(Modifier.height(8.dp))
                Text(a.scope, style = Small.copy(color = J.ink))
                Spacer(Modifier.height(10.dp))
                if (st.taken) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Sonuç: ", style = Small)
                        Text(st.score.ifBlank { "—" }, style = TitleM)
                        Spacer(Modifier.weight(1f))
                        GhostBtn("Geri al", {
                            store.setAssessment(a.id, st.copy(taken = false))
                        })
                    }
                } else {
                    Field(score, { score = it }, "Sonuç / not", single = true)
                    Spacer(Modifier.height(8.dp))
                    Btn("Girdim", {
                        store.setAssessment(a.id, st.copy(taken = true, score = score))
                    }, bg = J.forest, emoji = "✓", enabled = reached)
                    if (!reached) {
                        Spacer(Modifier.height(6.dp))
                        Text("Bu sınavın birimi henüz açılmadı.", style = Tiny)
                    }
                }
            }
        }
    }
}

// ══════════════════════════ WEEKLY REVIEW ══════════════════════════

@Composable
fun ReviewScreen(store: Store, onBack: () -> Unit) {
    var produced by remember { mutableStateOf("") }
    var canExplain by remember { mutableStateOf("") }
    var needsBook by remember { mutableStateOf("") }
    var declining by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, 12.dp, 14.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        item {
            GhostBtn("Geri", onBack, emoji = "←")
            Spacer(Modifier.height(10.dp))
            Text("📝 Haftalık Değerlendirme", style = Display)
            Text("Dört soru, beş dakika.", style = Small)
        }
        item {
            Card {
                Text("Bu hafta ne ürettim?", style = TitleM)
                Spacer(Modifier.height(6.dp))
                Field(produced, { produced = it }, "kod, yazı, çözüm, kayıt…", minLines = 2)
            }
        }
        item {
            Card {
                Text("Hangi konuyu kitapsız anlatabiliyorum?", style = TitleM)
                Spacer(Modifier.height(6.dp))
                Field(canExplain, { canExplain = it }, "…", minLines = 2)
            }
        }
        item {
            Card {
                Text("Hangi konuda hâlâ kitaba bakıyorum?", style = TitleM)
                Spacer(Modifier.height(6.dp))
                Field(needsBook, { needsBook = it }, "…", minLines = 2)
            }
        }
        item {
            Card(border = if (declining) J.butter else J.line) {
                Row(
                    Modifier.clickable { declining = !declining },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(declining, { declining = !declining }, J.butter)
                    Spacer(Modifier.width(10.dp))
                    Text("Metrikler düşüşte", style = Body)
                }
                if (declining) {
                    Spacer(Modifier.height(9.dp))
                    Text(
                        "Önerilen ayarlama: bu hafta yeni konu açma. Mevcut birimin görevlerini bitir, "
                            + "Hata Defteri'ni tam bir kez geç, dil bloğunu koru. Plan seni ezmek için değil, "
                            + "yol göstermek için var.",
                        style = Small.copy(color = J.ink)
                    )
                }
            }
        }
        item {
            Btn(if (saved) "Kaydedildi ✓" else "Değerlendirmeyi kaydet", {
                store.addReview(
                    WeeklyReview(uid(), produced, canExplain, needsBook, declining, System.currentTimeMillis())
                )
                saved = true
            }, bg = J.forest, emoji = "📝")
        }
        if (store.reviews.isNotEmpty()) {
            item { SectionLabel("geçmiş", "🕰️") }
            items(store.reviews) { r ->
                Card(border = J.lineSoft) {
                    Text(r.produced.ifBlank { "—" }, style = Body, maxLines = 3)
                    if (r.declining) { Spacer(Modifier.height(5.dp)); Chip("düşüş", J.butter) }
                }
            }
        }
    }
}

// ══════════════════════════ STORAGE ══════════════════════════

@Composable
fun StorageScreen(
    store: Store,
    vault: FileVault,
    compact: Boolean,
    onPickFolder: () -> Unit,
    onBack: () -> Unit
) {
    var usage by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var msg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(store.storageUri, store.recordings.size) {
        usage = try { vault.usage() } catch (_: Exception) { emptyMap() }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, 12.dp, 14.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        item {
            GhostBtn("Geri", onBack, emoji = "←")
            Spacer(Modifier.height(10.dp))
            Text("📁 Depolama ve Dosyalar", style = Display)
        }

        item {
            Card(border = J.forest.copy(alpha = 0.35f)) {
                Text("DIŞ DOSYA KONUMU", style = Tiny.copy(color = J.forest, fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(6.dp))
                Text(vault.displayPath(), style = Body)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (vault.isExternal) "✓ Kendi seçtiğin klasör kullanılıyor"
                    else "Şu an uygulama klasörü kullanılıyor. Kendi klasörünü seçebilirsin.",
                    style = Small
                )
                Spacer(Modifier.height(11.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Btn(
                        if (vault.isExternal) "Konumu değiştir" else "Klasör seç",
                        onPickFolder, Modifier.weight(1f), J.forest, emoji = "📂"
                    )
                }
            }
        }

        if (!compact) {
            item {
                Card(bg = J.paperDeep.copy(alpha = 0.5f), border = J.lineSoft) {
                    Text("Oluşturulan yapı", style = TitleM)
                    Spacer(Modifier.height(7.dp))
                    Text("(≧▽≦)/", style = Mono.copy(color = J.ink))
                    FileVault.FOLDERS.forEachIndexed { i, f ->
                        val prefix = if (i == FileVault.FOLDERS.size - 1) "└── " else "├── "
                        Text("$prefix$f/", style = Mono)
                    }
                }
            }

            item {
                SectionLabel("kullanım", "📊")
                Spacer(Modifier.height(7.dp))
                Card(border = J.lineSoft) {
                    FileVault.FOLDERS.forEach { f ->
                        Row(Modifier.padding(vertical = 3.dp)) {
                            Text(f, style = Body, modifier = Modifier.weight(1f))
                            Text(humanSize(usage[f] ?: 0L), style = Mono)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(J.line))
                    Spacer(Modifier.height(6.dp))
                    Row {
                        Text("Toplam", style = TitleM, modifier = Modifier.weight(1f))
                        Text(humanSize(usage.values.sum()), style = TitleM)
                    }
                }
            }

            item {
                SectionLabel("yedekleme", "💾")
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Btn("Yedek al", {
                        val uri = vault.writeText(
                            "Backups",
                            "yedek_${System.currentTimeMillis()}.json",
                            "application/json",
                            store.exportJson()
                        )
                        msg = if (uri != null) "Yedek alındı ✓" else "Yedek alınamadı"
                    }, Modifier.weight(1f), J.bark, emoji = "💾")
                    Btn("Müfredatı dışa aktar", {
                        val uri = vault.writeText(
                            "Exports", "mufredat.md", "text/markdown", exportCurriculum(store)
                        )
                        msg = if (uri != null) "Dışa aktarıldı ✓" else "Aktarılamadı"
                    }, Modifier.weight(1f), J.forest, emoji = "📤")
                }
                msg?.let { Spacer(Modifier.height(8.dp)); Text(it, style = Small.copy(color = J.forest)) }
            }

            item {
                Card(border = J.lineSoft) {
                    Text("Uygulama verisi ve dosyaların ayrı", style = TitleM)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "İlerlemen, notların ve hataların uygulama içinde tutulur. Ses kayıtların, "
                            + "dışa aktarmaların ve yedeklerin senin seçtiğin klasörde durur. "
                            + "Uygulamayı silip yeniden kursan bile aynı klasörü seçerek dosyalarına dönebilirsin.",
                        style = Small
                    )
                }
            }
        }
    }
}

private fun exportCurriculum(store: Store): String = buildString {
    val c = store.curriculum
    appendLine("# ${c.title}")
    appendLine()
    c.phases.forEach { p ->
        appendLine("## ${p.name} — ${p.sub}")
        appendLine(p.goal)
        appendLine()
        p.units.forEach { u ->
            val mark = if (store.isUnitComplete(u)) "x" else " "
            appendLine("### [$mark] ${u.title}  *(${u.kicker})*")
            u.tasks.forEach { t ->
                val d = if (store.done[t.id] == true) "x" else " "
                appendLine("- [$d] ${t.text}  `${t.subject}/${t.kind}·${t.minutes}dk`")
            }
            u.feynman?.let { appendLine("- 🎙️ Feynman: $it") }
            appendLine()
        }
    }
}

// ══════════════════════════ STUDY BAG ══════════════════════════

@Composable
fun StudyBagScreen(store: Store, onGo: (Screen) -> Unit, onExplainIt: () -> Unit) {
    val c = store.curriculum
    val items = listOf(
        Triple("🎙️", "Anlatımlarım", Screen.Audio),
        Triple("📋", "Sınavlar", Screen.Assessments),
        Triple("🍂", "Hata Defteri", Screen.Mistakes),
        Triple("📝", "Haftalık Değerlendirme", Screen.Review),
        Triple("📁", "Depolama ve Dosyalar", Screen.Storage),
        Triple("📚", "Kaynaklar", Screen.Resources)
    )

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, 12.dp, 14.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item {
            Text("🎒 Çanta", style = Display)
            Text("Küçük araçlar, tek yerde.", style = Small)
        }

        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(J.blush.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
                    .dashed(J.blush.copy(alpha = 0.7f))
                    .clickable { onExplainIt() }
                    .padding(14.dp)
            ) {
                Text("(≧▽≦) anlamıyor", style = TitleL)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Küçük yaratığa bir konuyu anlat. Sınav değil, sohbet.",
                    style = Small.copy(color = J.ink)
                )
            }
        }

        items(items) { (e, name, screen) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(J.card, RoundedCornerShape(14.dp))
                    .border(1.dp, J.line, RoundedCornerShape(14.dp))
                    .clickable { onGo(screen) }
                    .padding(13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(e, style = TextStyle(fontSize = 19.sp))
                Spacer(Modifier.width(12.dp))
                Text(name, style = TitleM, modifier = Modifier.weight(1f))
                Text("→", style = TextStyle(fontSize = 15.sp, color = J.inkFaint))
            }
        }

        item {
            SectionLabel("derslerin", "📊")
            Spacer(Modifier.height(7.dp))
        }
        items(c.subjects) { s ->
            val (d, t) = store.subjectDone(s.code)
            if (t > 0) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(J.card, RoundedCornerShape(13.dp))
                        .border(1.dp, J.lineSoft, RoundedCornerShape(13.dp))
                        .padding(11.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${s.emoji} ${s.name}", style = Body, modifier = Modifier.weight(1f))
                        Text("$d / $t", style = Mono)
                    }
                    Spacer(Modifier.height(6.dp))
                    Bar(d.toFloat() / t, subjectColor(s.color), 4)
                }
            }
        }
    }
}

@Composable
fun ResourcesScreen(store: Store, onBack: () -> Unit) {
    val c = store.curriculum
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, 12.dp, 14.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            GhostBtn("Geri", onBack, emoji = "←")
            Spacer(Modifier.height(10.dp))
            Text("📚 Kaynaklar", style = Display)
        }
        c.subjects.forEach { s ->
            val rs = c.resources.filter { it.subject == s.code }
            if (rs.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    SectionLabel(s.name, s.emoji)
                }
                items(rs) { r ->
                    Card(border = J.lineSoft) {
                        Text(r.name, style = Body)
                        if (r.use.isNotBlank()) Text(r.use, style = Tiny)
                    }
                }
            }
        }
    }
}

// ══════════════════════════ EXPLAIN IT ══════════════════════════

@Composable
fun ExplainItScreen(store: Store, onBack: () -> Unit, onRecord: (RecordRequest) -> Unit) {
    val unit = store.currentUnit
    var text by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .gingham()
            .padding(16.dp)
    ) {
        GhostBtn("Geri", onBack, emoji = "←")
        Spacer(Modifier.height(20.dp))

        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "(≧▽≦)",
                style = TextStyle(
                    fontSize = 38.sp, fontWeight = FontWeight.Bold, color = J.forest,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            )
            Spacer(Modifier.height(10.dp))
            Column(
                Modifier
                    .background(J.card, RoundedCornerShape(18.dp))
                    .border(1.dp, J.line, RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Text(
                    if (sent) "aa! şimdi biraz anladım galiba… ama şunu tekrar söyler misin?"
                    else "\"${unit?.title ?: "bu konu"}\" nedir? ben hiç anlamadım…",
                    style = Body
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Field(text, { text = it }, "Ona anlat…", minLines = 6)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn("Anlattım", { sent = true }, Modifier.weight(1f), J.forest, emoji = "💬")
            Btn("Sesli anlat", {
                onRecord(RecordRequest("(≧▽≦)'ye ${unit?.title ?: "konuyu"} anlat", unit?.id, null, false))
            }, Modifier.weight(1f), J.cherry, emoji = "🎙️")
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "Bir şeyi küçük bir yaratığa anlatabiliyorsan, gerçekten biliyorsundur. "
                + "Takıldığın yer, henüz kapanmamış yerdir.",
            style = Small
        )
    }
}
