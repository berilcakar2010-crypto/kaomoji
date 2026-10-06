package com.beril.kaomoji.ui.lab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.beril.kaomoji.ai.engine.AICapabilityGate
import com.beril.kaomoji.ai.engine.AIResult
import com.beril.kaomoji.lab.model.KnowledgeObjectEntity
import com.beril.kaomoji.lab.model.ProjectPayload
import com.beril.kaomoji.lab.repository.LabRepository
import com.beril.kaomoji.ui.Btn
import com.beril.kaomoji.ui.Display
import com.beril.kaomoji.ui.Empty
import com.beril.kaomoji.ui.Field
import com.beril.kaomoji.ui.GhostBtn
import com.beril.kaomoji.ui.J
import com.beril.kaomoji.ui.Small
import com.beril.kaomoji.ui.TitleL
import com.beril.kaomoji.ui.TitleM
import com.beril.kaomoji.ui.nav.dpadFocusable
import kotlinx.coroutines.launch

/**
 * Projeler (§21) — bir görev listesi değil. En önemli alan her zaman SIRADAKİ EYLEM; araştırma
 * sorusu sabit kalır (bir proje zamanla "neyi sorduğunu" unutmamalı), notlar serbestçe büyür.
 */
@Composable
fun ProjectsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { LabRepository(ctx) }
    val gate = remember { AICapabilityGate.forContext(ctx) }
    val scope = rememberCoroutineScope()

    var projects by remember { mutableStateOf<List<KnowledgeObjectEntity>>(emptyList()) }
    var openId by remember { mutableStateOf<String?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    var nextActionEdit by remember { mutableStateOf("") }
    var notesEdit by remember { mutableStateOf("") }
    var improving by remember { mutableStateOf(false) }
    var improved by remember { mutableStateOf<AIResult<String>?>(null) }
    var refreshTick by remember { mutableStateOf(0) }

    LaunchedEffect(refreshTick) { projects = repo.allProjects() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, 12.dp, 14.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            GhostBtn("Geri", onBack, emoji = "←")
            Spacer(Modifier.height(10.dp))
            Text("⚗️ Projeler", style = Display)
            Text("Dersler projelere hizmet eder, tersi değil. En önemli alan: SIRADAKİ EYLEM.", style = Small)
        }

        item {
            Btn(if (showForm) "Formu Kapat" else "+ Yeni Proje", { showForm = !showForm })
        }
        if (showForm) {
            item {
                Column {
                    Field(title, { title = it }, placeholder = "Proje adı")
                    Spacer(Modifier.height(6.dp))
                    Field(question, { question = it }, placeholder = "Araştırma sorusu")
                    Spacer(Modifier.height(8.dp))
                    Btn("Oluştur", {
                        if (title.isNotBlank() && question.isNotBlank()) {
                            scope.launch {
                                repo.createProject(title, question)
                                title = ""; question = ""; showForm = false
                                refreshTick++
                            }
                        }
                    }, emoji = "⚗️")
                }
            }
        }

        if (projects.isEmpty()) {
            item { Empty("⚗️", "Henüz proje yok", "Bir araştırma sorusu olan ilk projeni ekle.") }
        } else {
            items(projects, key = { it.id }) { proj ->
                val p = runCatching { ProjectPayload.fromJson(proj.payload) }.getOrNull()
                val isOpen = openId == proj.id
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(J.card, RoundedCornerShape(14.dp))
                        .dpadFocusable(onClick = {
                            openId = if (isOpen) null else proj.id
                            nextActionEdit = p?.nextAction ?: ""
                            notesEdit = p?.notes ?: ""
                        }, shape = RoundedCornerShape(14.dp))
                        .padding(13.dp),
                ) {
                    Text(proj.title, style = TitleL)
                    if (p != null) Text(p.researchQuestion, style = Small.copy(color = J.inkFaint))
                    if (p?.nextAction?.isNotBlank() == true) {
                        Spacer(Modifier.height(4.dp))
                        Text("SIRADAKİ EYLEM: ${p.nextAction}", style = Small)
                    }
                    if (isOpen) {
                        Spacer(Modifier.height(8.dp))
                        Text("Sıradaki eylemi güncelle", style = TitleM)
                        Field(nextActionEdit, { nextActionEdit = it }, placeholder = "Sıradaki eylem")
                        Spacer(Modifier.height(6.dp))
                        Field(notesEdit, { notesEdit = it }, placeholder = "Notlar", minLines = 2)
                        Spacer(Modifier.height(6.dp))
                        Btn(if (improving) "İsteniyor…" else "AI ile Yazımı İyileştir", {
                            if (!improving && notesEdit.isNotBlank()) {
                                improving = true
                                scope.launch {
                                    improved = gate.improveWriting(notesEdit, "netlik ve dilbilgisini düzelt, anlamı değiştirme")
                                    improving = false
                                }
                            }
                        }, enabled = !improving, emoji = "✏️")
                        AiResultView(improved) { text ->
                            Column {
                                Text(text, style = Small)
                                Spacer(Modifier.height(4.dp))
                                GhostBtn("Bu metni kullan", { notesEdit = text; improved = null }, emoji = "↩")
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Btn("Kaydet", {
                            scope.launch {
                                repo.updateProject(proj.id, notes = notesEdit, nextAction = nextActionEdit)
                                openId = null
                                improved = null
                                refreshTick++
                            }
                        }, emoji = "✓")
                    }
                }
            }
        }
    }
}
