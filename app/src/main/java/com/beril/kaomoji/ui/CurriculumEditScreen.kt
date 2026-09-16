package com.beril.kaomoji.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beril.kaomoji.data.*
import com.beril.kaomoji.ui.nav.dpadFocusable

/**
 * Müfredatı manuel olarak düzenleme özelliği. Faz → birim listesi buradan
 * seçilir; bir birime dokununca [UnitEditScreen] açılır. Tüm değişiklikler
 * [Store.updateCurriculum] ile hem bellekte hem `custom_curriculum.json`
 * dosyasında kalıcı hale gelir — AI müfredat üreticisiyle aynı mekanizma.
 */
@Composable
fun CurriculumEditScreen(store: Store, onOpenUnit: (String) -> Unit, onBack: () -> Unit) {
    val c = store.curriculum

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, 12.dp, 14.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            GhostBtn("Geri", onBack, emoji = "←")
            Spacer(Modifier.height(10.dp))
            Text("✏️ Müfredatı Düzenle", style = Display)
            Text(
                "Bir birime dokun: görev metnini, süresini, dersini değiştir, " +
                    "yeni görev ekle ya da sil.",
                style = Small
            )
        }

        c.phases.forEach { phase ->
            item {
                Spacer(Modifier.height(4.dp))
                SectionLabel(phase.name, "🗂️")
            }
            items(phase.units, key = { it.id }) { u ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(J.card, RoundedCornerShape(14.dp))
                        .border(1.dp, J.line, RoundedCornerShape(14.dp))
                        .dpadFocusable(onClick = { onOpenUnit(u.id) })
                        .padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(u.kicker, style = Tiny.copy(color = J.apple, fontWeight = FontWeight.Bold))
                        Text(u.title, style = TitleM)
                        Text("${u.tasks.size} görev", style = Tiny)
                    }
                    Text("✏️", style = TextStyle(fontSize = 16.sp))
                }
            }
        }
    }
}

/** Tek bir birimin görev listesini düzenler: metin, ders, tür, süre; ekleme/silme. */
@Composable
fun UnitEditScreen(store: Store, unitId: String, onBack: () -> Unit) {
    val c = store.curriculum
    val unit = c.allUnits.firstOrNull { it.id == unitId } ?: return

    var title by remember(unitId) { mutableStateOf(unit.title) }
    var kicker by remember(unitId) { mutableStateOf(unit.kicker) }
    var editingTaskId by remember { mutableStateOf<String?>(null) }
    var addingNew by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, 12.dp, 14.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            GhostBtn("Geri", onBack, emoji = "←")
            Spacer(Modifier.height(10.dp))
            Text("Birimi Düzenle", style = Display)
        }

        item {
            Card {
                Text("Başlık", style = Tiny.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(4.dp))
                Field(title, { title = it }, "Birim başlığı", single = true)
                Spacer(Modifier.height(8.dp))
                Text("Kısa etiket", style = Tiny.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(4.dp))
                Field(kicker, { kicker = it }, "Kısa etiket", single = true)
                Spacer(Modifier.height(8.dp))
                Btn("Başlığı kaydet", {
                    store.updateCurriculum(c.withUnitMetaUpdated(unitId, title, kicker, unit.note))
                }, bg = J.forest, emoji = "✓")
            }
        }

        item { SectionLabel("görevler (${unit.tasks.size})", "✅") }

        items(unit.tasks, key = { it.id }) { t ->
            if (editingTaskId == t.id) {
                TaskEditForm(
                    c = c,
                    initial = t,
                    onCancel = { editingTaskId = null },
                    onSave = { edited ->
                        store.updateCurriculum(c.withTaskUpdated(unitId, edited))
                        editingTaskId = null
                    },
                    onDelete = {
                        store.updateCurriculum(c.withTaskRemoved(unitId, t.id))
                        editingTaskId = null
                    }
                )
            } else {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(J.card, RoundedCornerShape(14.dp))
                        .border(1.dp, J.lineSoft, RoundedCornerShape(14.dp))
                        .dpadFocusable(onClick = { editingTaskId = t.id })
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(t.text, style = Body)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${c.subject(t.subject)?.name ?: t.subject} · ${c.kind(t.kind)?.name ?: t.kind} · ${t.minutes}′",
                            style = Tiny
                        )
                    }
                    Text("✏️", style = TextStyle(fontSize = 14.sp))
                }
            }
        }

        item {
            if (addingNew) {
                TaskEditForm(
                    c = c,
                    initial = Task(id = "t_" + uid(), text = "", subject = c.subjects.firstOrNull()?.code ?: "", kind = c.kinds.firstOrNull()?.code ?: "", minutes = 30),
                    onCancel = { addingNew = false },
                    onSave = { yeni ->
                        store.updateCurriculum(c.withTaskAdded(unitId, yeni))
                        addingNew = false
                    },
                    onDelete = null
                )
            } else {
                Btn("+ Yeni görev ekle", { addingNew = true }, bg = J.cherry)
            }
        }
    }
}

@Composable
private fun TaskEditForm(
    c: Curriculum,
    initial: Task,
    onCancel: () -> Unit,
    onSave: (Task) -> Unit,
    onDelete: (() -> Unit)?
) {
    var text by remember(initial.id) { mutableStateOf(initial.text) }
    var subject by remember(initial.id) { mutableStateOf(initial.subject) }
    var kind by remember(initial.id) { mutableStateOf(initial.kind) }
    var minutesText by remember(initial.id) { mutableStateOf(initial.minutes.toString()) }

    Card(border = J.cherry.copy(alpha = 0.4f)) {
        Text("Görev metni", style = Tiny.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(4.dp))
        Field(text, { text = it }, "Görev metni")

        Spacer(Modifier.height(8.dp))
        Text("Ders", style = Tiny.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(4.dp))
        Selector(c.subjects.map { it.code }, subject, { subject = it }) { code ->
            c.subject(code)?.let { "${it.emoji} ${it.name}" } ?: code
        }

        Spacer(Modifier.height(8.dp))
        Text("Tür", style = Tiny.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(4.dp))
        Selector(c.kinds.map { it.code }, kind, { kind = it }) { code ->
            c.kind(code)?.let { "${it.emoji} ${it.name}" } ?: code
        }

        Spacer(Modifier.height(8.dp))
        Text("Süre (dakika)", style = Tiny.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(4.dp))
        Field(minutesText, { minutesText = it.filter { ch -> ch.isDigit() } }, "30", single = true)

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(
                "Kaydet",
                {
                    if (text.isNotBlank() && subject.isNotBlank() && kind.isNotBlank()) {
                        onSave(
                            initial.copy(
                                text = text.trim(),
                                subject = subject,
                                kind = kind,
                                minutes = minutesText.toIntOrNull() ?: initial.minutes
                            )
                        )
                    }
                },
                Modifier.weight(1f), bg = J.forest, emoji = "✓"
            )
            GhostBtn("Vazgeç", onCancel)
        }
        if (onDelete != null) {
            Spacer(Modifier.height(8.dp))
            GhostBtn("Görevi sil", onDelete, color = J.cherry, emoji = "🗑️")
        }
    }
}
