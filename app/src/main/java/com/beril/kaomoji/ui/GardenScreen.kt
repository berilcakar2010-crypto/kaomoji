package com.beril.kaomoji.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beril.kaomoji.audio.fmtDuration
import com.beril.kaomoji.data.*

private val stages = listOf(
    "🌰" to "tohum",
    "🌱" to "filiz",
    "🪴" to "fidan",
    "🌳" to "ağaç",
    "🍎" to "meyve"
)

@Composable
fun GardenScreen(
    store: Store,
    wide: Boolean,
    onOpenUnit: (String) -> Unit,
    onGo: (Screen) -> Unit,
    onRecord: (RecordRequest) -> Unit,
    onOpenProject: (String) -> Unit
) {
    val c = store.curriculum
    val unit = store.currentUnit
    val phase = store.currentPhase
    val mission = remember(
        store.done.size, store.skipped.size, store.recordings.size,
        store.mistakes.size, store.inbox.size, store.missionOverride
    ) { MissionEngine.pick(store) }

    var showAlts by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .gingham(),
        contentPadding = PaddingValues(14.dp, 10.dp, 14.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // ── header ──
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "(≧▽≦)",
                    style = TextStyle(
                        fontSize = 34.sp, fontWeight = FontWeight.Bold,
                        color = J.forest, fontFamily = FontFamily.Monospace
                    )
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "küçük adımlar, büyük ağaçlar",
                    style = Small.copy(color = J.inkFaint)
                )
            }
        }

        // ── garden strip ──
        item { GardenStrip(store) }

        // ── TODAY'S TINY MISSION ──
        item {
            SectionLabel("bugünün küçük görevi", "🍀")
            Spacer(Modifier.height(8.dp))
            MissionCard(
                mission = mission,
                store = store,
                onDone = { mission.taskId?.let { store.toggleTask(it) } },
                onRecord = {
                    onRecord(
                        RecordRequest(
                            prompt = mission.recordingPrompt ?: mission.title,
                            unitId = mission.unitId,
                            taskId = mission.taskId,
                            isFeynman = mission.recordingPrompt != null
                        )
                    )
                },
                onOpen = {
                    when {
                        mission.assessmentId != null -> onGo(Screen.Assessments)
                        mission.projectId != null -> onOpenProject(mission.projectId)
                        mission.mistakeId != null -> onGo(Screen.Mistakes)
                        mission.kind == "review" && mission.taskId == null -> onGo(Screen.Review)
                        mission.unitId != null -> onOpenUnit(mission.unitId)
                        else -> {}
                    }
                },
                onShuffle = { showAlts = !showAlts }
            )
            if (showAlts) {
                Spacer(Modifier.height(8.dp))
                MissionEngine.alternatives(store, mission).forEach { alt ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .background(J.card, RoundedCornerShape(12.dp))
                            .border(1.dp, J.lineSoft, RoundedCornerShape(12.dp))
                            .clickable {
                                if (alt.recordingPrompt != null)
                                    onRecord(RecordRequest(alt.recordingPrompt, alt.unitId, null, true))
                                else alt.taskId?.let { store.toggleTask(it) }
                                showAlts = false
                            }
                            .padding(11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(alt.emoji, style = TextStyle(fontSize = 16.sp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(alt.title, style = Body, maxLines = 2)
                            Text(alt.why, style = Tiny)
                        }
                    }
                }
            }
        }

        // ── continue ──
        if (unit != null) {
            item {
                SectionLabel("devam et", "🌱")
                Spacer(Modifier.height(8.dp))
                ContinueCard(store, unit, phase) { onOpenUnit(unit.id) }
            }
        }

        // ── quick actions ──
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn(
                    "Anlat", { onRecord(RecordRequest(null, unit?.id, null, false)) },
                    Modifier.weight(1f), J.cherry, emoji = "🎙️"
                )
                Btn(
                    "Yakala", { onGo(Screen.Inbox) },
                    Modifier.weight(1f), J.bark, emoji = "🧺"
                )
                Btn(
                    "Hata", { onGo(Screen.Mistakes) },
                    Modifier.weight(1f), J.butter, Color(0xFF3A2F16), emoji = "🍂"
                )
            }
        }

        // ── stats ──
        item {
            SectionLabel("bu güne kadar", "📊")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("${store.totalDone}", "görev", "✅", J.forest)
                StatTile("${store.problemsSolved}", "problem", "✏️", J.cherry)
                StatTile("${store.recordings.size}", "anlatım", "🎙️", J.lilac)
                StatTile(
                    "${store.mistakes.count { !it.resolved }}", "açık hata", "🍂", J.butter
                )
            }
        }

        // ── recent explanation ──
        item {
            SectionLabel("son anlatım", "🎙️")
            Spacer(Modifier.height(8.dp))
            val r = store.recordings.firstOrNull()
            if (r == null) {
                Card(border = J.lineSoft) {
                    Text("Henüz kayıt yok.", style = BodySoft)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Bir konuyu kitaba bakmadan anlatmak, onu bildiğini kanıtlamanın tek yolu.",
                        style = Small
                    )
                    Spacer(Modifier.height(10.dp))
                    Btn("İlk kaydını al", { onRecord(RecordRequest(null, unit?.id, null, false)) }, emoji = "🎙️")
                }
            } else {
                Card(
                    Modifier.clickable { onGo(Screen.Audio) },
                    border = J.lilac.copy(alpha = 0.4f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Sticker("🎙️", 34, J.lilac.copy(alpha = 0.25f))
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(r.title, style = TitleM, maxLines = 1)
                            Text(
                                buildString {
                                    append(fmtDuration(r.durationMs))
                                    r.unitId?.let { u ->
                                        c.allUnits.firstOrNull { it.id == u }?.let { append(" · ${it.title}") }
                                    }
                                },
                                style = Small, maxLines = 1
                            )
                        }
                        if (r.needsReview) Chip("gözden geçir", J.butter)
                    }
                }
            }
        }

        // ── active projects ──
        item {
            SectionLabel("aktif projeler", "🌿")
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                items(c.projects) { p ->
                    val st = store.projectStates[p.id]
                    Column(
                        Modifier
                            .width(190.dp)
                            .background(J.card, RoundedCornerShape(16.dp))
                            .border(1.dp, J.line, RoundedCornerShape(16.dp))
                            .clickable { onOpenProject(p.id) }
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(p.emoji, style = TextStyle(fontSize = 16.sp))
                            Spacer(Modifier.width(6.dp))
                            Text(p.name, style = TitleM, maxLines = 1)
                        }
                        Spacer(Modifier.height(7.dp))
                        Text("SIRADAKİ", style = Tiny.copy(color = J.apple, fontWeight = FontWeight.Bold))
                        Text(
                            st?.nextAction?.ifBlank { p.defaultNext } ?: p.defaultNext,
                            style = Small.copy(color = J.ink), maxLines = 3
                        )
                    }
                }
            }
        }

        // ── inbox peek ──
        item {
            val open = store.inbox.filter { !it.done }
            SectionLabel("brain inbox", "🧺")
            Spacer(Modifier.height(8.dp))
            Card(Modifier.clickable { onGo(Screen.Inbox) }, border = J.bark.copy(alpha = 0.3f)) {
                if (open.isEmpty()) {
                    Text("Aklında ne var?", style = BodySoft)
                    Text("Düşünceyi yakala, düzenlemeyi sonra yap.", style = Small)
                } else {
                    Text("${open.size} not bekliyor", style = TitleM)
                    Spacer(Modifier.height(6.dp))
                    open.take(3).forEach {
                        Text("· ${it.text}", style = Small.copy(color = J.ink), maxLines = 1)
                    }
                }
            }
        }

        // ── bridges ──
        item {
            SectionLabel("disiplinlerarası köprüler", "🔗")
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                items(c.bridges) { b ->
                    Column(
                        Modifier
                            .width(215.dp)
                            .background(J.mint.copy(alpha = 0.20f), RoundedCornerShape(16.dp))
                            .dashed(J.forest.copy(alpha = 0.3f))
                            .padding(12.dp)
                    ) {
                        Text("${b.emoji}  ${b.name}", style = TitleM.copy(fontSize = 14.sp))
                        Spacer(Modifier.height(5.dp))
                        Text(b.desc, style = Small, maxLines = 4)
                    }
                }
            }
        }
    }
}

@Composable
private fun GardenStrip(store: Store) {
    val stage = store.growthStage
    val (emoji, name) = stages[stage]
    val c = store.curriculum
    val doneUnits = store.currentUnitIndex
    val total = c.allUnits.size

    Column(
        Modifier
            .fillMaxWidth()
            .background(J.mint.copy(alpha = 0.22f), RoundedCornerShape(20.dp))
            .border(1.dp, J.apple.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, style = TextStyle(fontSize = 40.sp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(store.currentPhase?.name ?: "Bahçe", style = TitleL)
                Text(
                    "$doneUnits / $total birim · $name aşaması",
                    style = Small
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                stages.forEachIndexed { i, (e, _) ->
                    if (i <= stage) Text(
                        e,
                        style = TextStyle(fontSize = 11.sp),
                        modifier = Modifier.rotate((i * 7 - 10).toFloat())
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Bar(doneUnits.toFloat() / total.coerceAtLeast(1), J.apple, 7)
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            c.phases.forEach { p ->
                val pr = store.phaseProgress(p)
                Column(Modifier.weight(1f)) {
                    Text(p.name, style = Tiny, maxLines = 1)
                    Spacer(Modifier.height(3.dp))
                    Bar(pr, if (pr >= 1f) J.cherry else J.forest, 4, J.paper)
                }
            }
        }
    }
}

@Composable
private fun MissionCard(
    mission: Mission,
    store: Store,
    onDone: () -> Unit,
    onRecord: () -> Unit,
    onOpen: () -> Unit,
    onShuffle: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(J.card, RoundedCornerShape(20.dp))
            .border(2.dp, J.cherry.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(mission.emoji, style = TextStyle(fontSize = 26.sp))
            Spacer(Modifier.width(11.dp))
            Text(mission.title, style = TitleL.copy(fontSize = 17.sp), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .background(J.paperDeep.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            Text("💭", style = TextStyle(fontSize = 12.sp))
            Spacer(Modifier.width(8.dp))
            Text(mission.why, style = Small.copy(color = J.inkSoft))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                mission.kind == "explain" || mission.recordingPrompt != null ->
                    Btn("Kaydet", onRecord, Modifier.weight(1f), J.cherry, emoji = "🎙️")

                mission.taskId != null ->
                    Btn("Bitirdim", onDone, Modifier.weight(1f), J.forest, emoji = "✓")

                else ->
                    Btn("Aç", onOpen, Modifier.weight(1f), J.forest, emoji = "→")
            }
            if (mission.taskId != null) GhostBtn("Aç", onOpen, emoji = "📚")
            GhostBtn("Başka", onShuffle, emoji = "🔄")
        }
    }
}

@Composable
private fun ContinueCard(store: Store, unit: CurriculumUnit, phase: Phase?, onOpen: () -> Unit) {
    val pr = store.unitProgress(unit)
    val remaining = unit.tasks.count { store.done[it.id] != true && store.skipped[it.id] != true }
    Column(
        Modifier
            .fillMaxWidth()
            .background(J.card, RoundedCornerShape(18.dp))
            .border(1.dp, J.line, RoundedCornerShape(18.dp))
            .clickable { onOpen() }
            .padding(14.dp)
    ) {
        Text(unit.kicker.uppercase(), style = Tiny.copy(color = J.apple, fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(4.dp))
        Text(unit.title, style = TitleL)
        Spacer(Modifier.height(9.dp))
        Bar(pr, J.forest)
        Spacer(Modifier.height(7.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${(pr * 100).toInt()}% · $remaining görev kaldı",
                style = Small, modifier = Modifier.weight(1f)
            )
            if (unit.bridges.isNotEmpty()) Chip("🔗 köprü", J.forest)
        }
    }
}
