package com.beril.kaomoji.ui.lab2

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
import com.beril.kaomoji.lab.model.KnowledgeObjectEntity
import com.beril.kaomoji.lab.model.MistakePayload
import com.beril.kaomoji.lab.repository.LabRepository
import com.beril.kaomoji.ui.Btn
import com.beril.kaomoji.ui.Display
import com.beril.kaomoji.ui.Empty
import com.beril.kaomoji.ui.Field
import com.beril.kaomoji.ui.GhostBtn
import com.beril.kaomoji.ui.J
import com.beril.kaomoji.ui.SectionLabel
import com.beril.kaomoji.ui.Small
import com.beril.kaomoji.ui.TitleM
import kotlinx.coroutines.launch

/**
 * Hata Defteri (§24) — her hata soru/deneme/ne-yanlış-gitti/neden/doğru-akıl-yürütme/kategori
 * ile kayıtlı. Suçlamaz, sadece örüntü gösterir: "$n hata $kategori kategorisinde" gibi bir
 * cümle, "kötüsün" demeden.
 */
@Composable
fun MistakeJournalScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { LabRepository(ctx) }
    val scope = rememberCoroutineScope()

    var mistakes by remember { mutableStateOf<List<KnowledgeObjectEntity>>(emptyList()) }
    var categoryCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var showForm by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf("") }
    var attempt by remember { mutableStateOf("") }
    var whatWentWrong by remember { mutableStateOf("") }
    var whyItHappened by remember { mutableStateOf("") }
    var correctReasoning by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var refreshTick by remember { mutableStateOf(0) }

    LaunchedEffect(refreshTick) {
        mistakes = repo.recentMistakes()
        categoryCounts = repo.mistakeCategoryCounts()
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, 12.dp, 14.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            GhostBtn("Geri", onBack, emoji = "←")
            Spacer(Modifier.height(10.dp))
            Text("⚠️ Hata Defteri", style = Display)
            Text("Her hata veri. Soru / deneme / ne yanlış gitti / neden / doğru akıl yürütme.", style = Small)
        }

        val recurring = categoryCounts.filter { it.value >= 2 }
        if (recurring.isNotEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().background(J.butter.copy(alpha = 0.25f), RoundedCornerShape(12.dp)).padding(12.dp)) {
                    Text("Tekrarlayan örüntü", style = TitleM)
                    recurring.forEach { (cat, n) -> Text("$n hata \"$cat\" kategorisinde — burada kapanmamış bir şey olabilir.", style = Small) }
                }
            }
        }

        item {
            Btn(if (showForm) "Formu Kapat" else "+ Yeni Hata Ekle", { showForm = !showForm })
        }
        if (showForm) {
            item {
                Column {
                    Field(problem, { problem = it }, placeholder = "Soru / problem")
                    Spacer(Modifier.height(6.dp))
                    Field(attempt, { attempt = it }, placeholder = "Kendi denemen")
                    Spacer(Modifier.height(6.dp))
                    Field(whatWentWrong, { whatWentWrong = it }, placeholder = "Ne yanlış gitti")
                    Spacer(Modifier.height(6.dp))
                    Field(whyItHappened, { whyItHappened = it }, placeholder = "Neden oldu")
                    Spacer(Modifier.height(6.dp))
                    Field(correctReasoning, { correctReasoning = it }, placeholder = "Doğru akıl yürütme")
                    Spacer(Modifier.height(6.dp))
                    Field(category, { category = it }, placeholder = "Kategori (örn. sign-error)", single = true)
                    Spacer(Modifier.height(8.dp))
                    Btn("Kaydet", {
                        if (problem.isNotBlank()) {
                            scope.launch {
                                repo.logMistake(
                                    MistakePayload(problem, attempt, whatWentWrong, whyItHappened, correctReasoning, category.ifBlank { "diğer" }),
                                )
                                problem = ""; attempt = ""; whatWentWrong = ""; whyItHappened = ""; correctReasoning = ""; category = ""
                                showForm = false
                                refreshTick++
                            }
                        }
                    }, emoji = "✓")
                }
            }
        }

        item { SectionLabel("son hatalar", "⚠️") }
        if (mistakes.isEmpty()) {
            item { Empty("⚠️", "Henüz hata kaydı yok", "Bir soruda takıldığında ya da yanlış yaptığında buraya ekle — bu ileride en çok işine yarayacak araç.") }
        } else {
            items(mistakes, key = { it.id }) { m ->
                val p = runCatching { MistakePayload.fromJson(m.payload) }.getOrNull()
                Column(
                    Modifier.fillMaxWidth().background(J.card, RoundedCornerShape(14.dp)).padding(13.dp),
                ) {
                    Text(m.title, style = TitleM)
                    if (p != null) {
                        Text("Ne yanlış gitti: ${p.whatWentWrong}", style = Small)
                        Text("Doğrusu: ${p.correctReasoning}", style = Small.copy(color = J.inkFaint))
                        Text(p.category, style = Small.copy(color = J.inkFaint))
                    }
                }
            }
        }
    }
}
