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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beril.kaomoji.data.*

@Composable
fun CurriculumScreen(store: Store, onOpenUnit: (String) -> Unit) {
    val c = store.curriculum
    var openPhase by remember { mutableStateOf(store.currentPhase?.id ?: "p1") }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, 12.dp, 14.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("📚 Müfredat", style = Display)
            Text(
                "${c.allUnits.size} birim · ${c.totalTasks} görev · zamansız akış",
                style = Small
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Bir birimin görevleri bitince sıradaki açılır. Tarih yok, gecikme yok.",
                style = Small.copy(color = J.inkFaint)
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(c.phases) { p ->
                    val on = p.id == openPhase
                    val pr = store.phaseProgress(p)
                    Column(
                        Modifier
                            .width(140.dp)
                            .background(
                                if (on) J.forest else J.card,
                                RoundedCornerShape(14.dp)
                            )
                            .border(1.dp, if (on) J.forest else J.line, RoundedCornerShape(14.dp))
                            .clickable { openPhase = p.id }
                            .padding(11.dp)
                    ) {
                        Text(
                            p.name,
                            style = TitleM.copy(
                                color = if (on) Color.White else J.ink,
                                fontSize = 14.sp
                            ), maxLines = 1
                        )
                        Text(
                            p.sub,
                            style = Tiny.copy(color = if (on) Color.White.copy(alpha = 0.75f) else J.inkFaint)
                        )
                        Spacer(Modifier.height(7.dp))
                        Bar(pr, if (on) J.lime else J.apple, 4, if (on) Color.White.copy(alpha = 0.25f) else J.paperDeep)
                    }
                }
            }
        }

        val phase = c.phases.firstOrNull { it.id == openPhase }
        if (phase != null) {
            item {
                Card(bg = J.paperDeep.copy(alpha = 0.55f), border = J.lineSoft) {
                    Text(phase.goal, style = Body)
                    Spacer(Modifier.height(5.dp))
                    Text("~${phase.hours} saat · ${phase.units.size} birim", style = Tiny)
                }
            }

            items(phase.units) { u ->
                val unlocked = store.isUnlocked(u)
                val complete = store.isUnitComplete(u)
                val current = u.id == store.currentUnit?.id
                UnitRow(store, u, unlocked, complete, current) { if (unlocked) onOpenUnit(u.id) }
            }
        }
    }
}

@Composable
private fun UnitRow(
    store: Store,
    u: CurriculumUnit,
    unlocked: Boolean,
    complete: Boolean,
    current: Boolean,
    onOpen: () -> Unit
) {
    val pr = store.unitProgress(u)
    val border = when {
        current -> J.cherry.copy(alpha = 0.5f)
        complete -> J.apple.copy(alpha = 0.4f)
        else -> J.lineSoft
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(
                if (unlocked) J.card else J.paperDeep.copy(alpha = 0.35f),
                RoundedCornerShape(16.dp)
            )
            .border(if (current) 2.dp else 1.dp, border, RoundedCornerShape(16.dp))
            .clickable(enabled = unlocked) { onOpen() }
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            when {
                complete -> "🍎"
                current -> "🌱"
                unlocked -> "🌿"
                else -> "🌰"
            },
            style = TextStyle(fontSize = 21.sp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    u.kicker,
                    style = Tiny.copy(color = if (unlocked) J.apple else J.inkFaint, fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                if (current) Chip("şimdi", J.cherry)
            }
            Spacer(Modifier.height(2.dp))
            Text(
                u.title,
                style = TitleM.copy(color = if (unlocked) J.ink else J.inkFaint)
            )
            Spacer(Modifier.height(6.dp))
            if (unlocked) {
                Bar(pr, if (complete) J.apple else J.forest, 4)
                Spacer(Modifier.height(4.dp))
                Text("${u.tasks.size} görev · ${(pr * 100).toInt()}%", style = Tiny)
            } else {
                Text("önceki birim bitince açılır", style = Tiny)
            }
        }
    }
}

@Composable
fun UnitDetailScreen(
    store: Store,
    unitId: String,
    onBack: () -> Unit,
    onRecord: (RecordRequest) -> Unit,
    onAddMistake: (String?) -> Unit,
    onLogProblems: (String) -> Unit
) {
    val c = store.curriculum
    val u = c.allUnits.firstOrNull { it.id == unitId } ?: return
    val phase = c.phaseOf(u.id)
    val unlocked = store.isUnlocked(u)
    val pr = store.unitProgress(u)
    val feyRec = store.recordings.firstOrNull { it.unitId == u.id && it.isFeynman }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, 12.dp, 14.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            GhostBtn("Geri", onBack, emoji = "←")
            Spacer(Modifier.height(10.dp))
            Text(
                "${phase?.name ?: ""} · ${u.kicker}".uppercase(),
                style = Tiny.copy(color = J.apple, fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(3.dp))
            Text(u.title, style = Display)
            Spacer(Modifier.height(10.dp))
            Bar(pr, J.forest, 7)
            Spacer(Modifier.height(6.dp))
            Text(
                "${u.tasks.count { store.done[it.id] == true }} / ${u.tasks.size} görev tamam",
                style = Small
            )
        }

        if (u.bridges.isNotEmpty()) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(J.mint.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                        .dashed(J.forest.copy(alpha = 0.3f), radius = 14f)
                        .padding(12.dp)
                ) {
                    Text("🔗 KÖPRÜ", style = Tiny.copy(color = J.forest, fontWeight = FontWeight.Bold))
                    Spacer(Modifier.height(5.dp))
                    u.bridges.forEach { name ->
                        val b = c.bridges.firstOrNull { it.name == name }
                        Text("${b?.emoji ?: "·"} $name", style = Body)
                        if (b != null) Text(b.desc, style = Small)
                        Spacer(Modifier.height(5.dp))
                    }
                }
            }
        }

        if (u.feynman != null) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(J.lilac.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                        .border(1.dp, J.lilac.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                        .padding(13.dp)
                ) {
                    Text("🎙️ FEYNMAN", style = Tiny.copy(color = J.lilac, fontWeight = FontWeight.Bold))
                    Spacer(Modifier.height(5.dp))
                    Text(u.feynman, style = Body)
                    Spacer(Modifier.height(10.dp))
                    if (feyRec != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✓ kaydedildi:", style = Small.copy(color = J.forest))
                            Spacer(Modifier.width(5.dp))
                            Text(feyRec.title, style = Small.copy(color = J.ink), maxLines = 1)
                        }
                        Spacer(Modifier.height(8.dp))
                        GhostBtn("Yeniden anlat", {
                            onRecord(RecordRequest(u.feynman, u.id, null, true))
                        }, emoji = "🔁")
                    } else {
                        Btn("Anlat ve kaydet", {
                            onRecord(RecordRequest(u.feynman, u.id, null, true))
                        }, bg = J.lilac, emoji = "🎙️")
                    }
                }
            }
        }

        item { SectionLabel("görevler", "✅") }

        items(u.tasks) { t ->
            TaskRow(
                task = t, c = c,
                done = store.done[t.id] == true,
                skipped = store.skipped[t.id] == true,
                enabled = unlocked,
                onToggle = { store.toggleTask(t.id) },
                onSkip = { store.skipTask(t.id) },
                onExplain = { onRecord(RecordRequest(t.text, u.id, t.id, false)) }
            )
        }

        item {
            Spacer(Modifier.height(6.dp))
            SectionLabel("bu birim için", "🍂")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn("Hata ekle", { onAddMistake(u.id) }, Modifier.weight(1f), J.butter, Color(0xFF3A2F16), "🍂")
                Btn("Problem kaydet", { onLogProblems(u.id) }, Modifier.weight(1f), J.cherry, emoji = "✏️")
            }
        }

        val related = store.mistakes.filter { it.unitId == u.id }
        if (related.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Text("Bu birimdeki hataların (${related.size})", style = TitleM)
            }
            items(related) { m ->
                Card(border = J.butter.copy(alpha = 0.4f)) {
                    Text(m.problem, style = Body, maxLines = 2)
                    Spacer(Modifier.height(4.dp))
                    Chip(m.category, J.butter)
                }
            }
        }

        val recs = store.recordings.filter { it.unitId == u.id }
        if (recs.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Text("Bu birimdeki anlatımların (${recs.size})", style = TitleM)
            }
            items(recs) { r ->
                Card(border = J.lilac.copy(alpha = 0.35f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎙️", style = TextStyle(fontSize = 15.sp))
                        Spacer(Modifier.width(9.dp))
                        Text(r.title, style = Body, modifier = Modifier.weight(1f), maxLines = 1)
                        Text(com.beril.kaomoji.audio.fmtDuration(r.durationMs), style = Mono)
                    }
                }
            }
        }
    }
}
