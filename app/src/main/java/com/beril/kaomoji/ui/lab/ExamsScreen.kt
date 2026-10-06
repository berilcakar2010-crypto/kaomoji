package com.beril.kaomoji.ui.lab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.beril.kaomoji.lab.model.ExamPayload
import com.beril.kaomoji.lab.model.KnowledgeObjectEntity
import com.beril.kaomoji.lab.model.ObjectKind
import com.beril.kaomoji.lab.repository.LabRepository
import com.beril.kaomoji.ui.Btn
import com.beril.kaomoji.ui.Display
import com.beril.kaomoji.ui.Empty
import com.beril.kaomoji.ui.Field
import com.beril.kaomoji.ui.GhostBtn
import com.beril.kaomoji.ui.J
import com.beril.kaomoji.ui.Selector
import com.beril.kaomoji.ui.Small
import com.beril.kaomoji.ui.TitleM
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Sınavlar / Ödevler (§20) — "bu sınav için ne önemli?" sorusuna cevap, günlük zorunlu
 * görevlere çevirmeden. Tarih [com.beril.kaomoji.lab.model.Schedule]'da — LabRepository.upcoming()
 * zaten bunu LabHomeScreen'in "şu an önemli olan" bölümünde gösteriyor; bu ekran yönetimi.
 */
@Composable
fun ExamsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { LabRepository(ctx) }
    val scope = rememberCoroutineScope()

    var exams by remember { mutableStateOf<List<KnowledgeObjectEntity>>(emptyList()) }
    var showForm by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var examScope by remember { mutableStateOf("") }
    var daysFromNow by remember { mutableStateOf("7") }
    var refreshTick by remember { mutableStateOf(0) }

    LaunchedEffect(refreshTick) { exams = repo.allExams() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, 12.dp, 14.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            GhostBtn("Geri", onBack, emoji = "←")
            Spacer(Modifier.height(10.dp))
            Text("📋 Sınavlar / Ödevler", style = Display)
        }

        item {
            Btn(if (showForm) "Formu Kapat" else "+ Yeni Sınav/Ödev", { showForm = !showForm })
        }
        if (showForm) {
            item {
                Column {
                    Field(title, { title = it }, placeholder = "Başlık (örn. \"Fizik Ara Sınavı\")")
                    Spacer(Modifier.height(6.dp))
                    Field(examScope, { examScope = it }, placeholder = "Kapsam (örn. \"Elektromanyetizma\")")
                    Spacer(Modifier.height(6.dp))
                    Field(daysFromNow, { daysFromNow = it }, placeholder = "Bugünden kaç gün sonra?", single = true)
                    Spacer(Modifier.height(8.dp))
                    Btn("Oluştur", {
                        val days = daysFromNow.toLongOrNull()
                        if (title.isNotBlank() && days != null) {
                            scope.launch {
                                repo.createExam(title, examScope, examDate = LocalDate.now().plusDays(days))
                                title = ""; examScope = ""; daysFromNow = "7"; showForm = false
                                refreshTick++
                            }
                        }
                    }, emoji = "📋")
                }
            }
        }

        if (exams.isEmpty()) {
            item { Empty("📋", "Henüz sınav/ödev yok", "Yaklaşan bir sınav eklediğinde burada ve Ana Sayfa'da görünecek.") }
        } else {
            items(exams, key = { it.id }) { exam ->
                val p = runCatching { ExamPayload.fromJson(exam.payload) }.getOrNull()
                val dateLabel = exam.schedule?.examDate?.let { LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofPattern("d MMMM", Locale("tr"))) }
                Column(
                    Modifier.fillMaxWidth().background(J.card, RoundedCornerShape(14.dp)).padding(13.dp),
                ) {
                    Row {
                        Text(if (exam.kind == ObjectKind.ASSIGNMENT) "📝" else "📋", style = Small)
                        Spacer(Modifier.width(8.dp))
                        Text(exam.title, style = TitleM)
                    }
                    if (dateLabel != null) Text(dateLabel, style = Small.copy(color = J.inkFaint))
                    if (p != null) {
                        Text(p.scope, style = Small)
                        Spacer(Modifier.height(6.dp))
                        Selector(
                            options = listOf("not-started", "in-progress", "ready"),
                            selected = p.prepStatus,
                            onSelect = { scope.launch { repo.updateExamPrepStatus(exam.id, it); refreshTick++ } },
                            labels = { prepStatusLabel(it) },
                        )
                    }
                }
            }
        }
    }
}

private fun prepStatusLabel(s: String): String = when (s) {
    "not-started" -> "Başlanmadı"
    "in-progress" -> "Sürüyor"
    "ready" -> "Hazır"
    else -> s
}
