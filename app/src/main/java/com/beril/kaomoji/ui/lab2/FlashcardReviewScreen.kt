package com.beril.kaomoji.ui.lab2

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
import androidx.compose.foundation.lazy.LazyColumn
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
import com.beril.kaomoji.lab.model.FlashcardPayload
import com.beril.kaomoji.lab.model.KnowledgeObjectEntity
import com.beril.kaomoji.lab.repository.LabRepository
import com.beril.kaomoji.ui.Btn
import com.beril.kaomoji.ui.Display
import com.beril.kaomoji.ui.Empty
import com.beril.kaomoji.ui.Field
import com.beril.kaomoji.ui.GhostBtn
import com.beril.kaomoji.ui.J
import com.beril.kaomoji.ui.SectionLabel
import com.beril.kaomoji.ui.Small
import com.beril.kaomoji.ui.TitleL
import com.beril.kaomoji.ui.TitleM
import kotlinx.coroutines.launch

/**
 * Tekrar kartları (§23) — aralıklı tekrar bir bileşen, öğrenmenin tanımı değil. Kullanıcı
 * her zaman NEDEN bir kartın tekrar edildiğini görür (vadesi geldiği için, bir süs/oyun
 * mekaniği olarak değil).
 */
@Composable
fun FlashcardReviewScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { LabRepository(ctx) }
    val gate = remember { AICapabilityGate.forContext(ctx) }
    val scope = rememberCoroutineScope()

    var due by remember { mutableStateOf<List<KnowledgeObjectEntity>>(emptyList()) }
    var totalCount by remember { mutableStateOf(0) }
    var reviewIndex by remember { mutableStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var showForm by remember { mutableStateOf(false) }
    var front by remember { mutableStateOf("") }
    var back by remember { mutableStateOf("") }
    var refreshTick by remember { mutableStateOf(0) }

    var showGenForm by remember { mutableStateOf(false) }
    var genSubject by remember { mutableStateOf("") }
    var genSourceText by remember { mutableStateOf("") }
    var generating by remember { mutableStateOf(false) }
    var genResult by remember { mutableStateOf<AIResult<List<Pair<String, String>>>?>(null) }

    LaunchedEffect(refreshTick) {
        due = repo.dueFlashcards()
        totalCount = repo.allFlashcards().size
        reviewIndex = 0
        revealed = false
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, 12.dp, 14.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            GhostBtn("Geri", onBack, emoji = "←")
            Spacer(Modifier.height(10.dp))
            Text("🃏 Tekrar Kartları", style = Display)
            Text("$totalCount kart, ${due.size} tanesinin vadesi geldi (SM-2).", style = Small)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn(if (showForm) "Formu Kapat" else "+ Kart Ekle", { showForm = !showForm })
                GhostBtn(if (showGenForm) "AI Formunu Kapat" else "AI'dan Kart Üret", { showGenForm = !showGenForm }, emoji = "🤖")
            }
        }
        if (showForm) {
            item {
                Column {
                    Field(front, { front = it }, placeholder = "Soru (ön yüz)")
                    Spacer(Modifier.height(6.dp))
                    Field(back, { back = it }, placeholder = "Cevap (arka yüz)")
                    Spacer(Modifier.height(8.dp))
                    Btn("Ekle", {
                        if (front.isNotBlank() && back.isNotBlank()) {
                            scope.launch {
                                repo.createFlashcard(front, back)
                                front = ""; back = ""; showForm = false
                                refreshTick++
                            }
                        }
                    }, emoji = "🃏")
                }
            }
        }
        if (showGenForm) {
            item {
                Column {
                    Text(
                        "Bir ders metni/notu yapıştır — AI soru-cevap kartı önerir, sen " +
                            "onaylamadan hiçbiri eklenmez.",
                        style = Small,
                    )
                    Spacer(Modifier.height(6.dp))
                    Field(genSubject, { genSubject = it }, placeholder = "Konu/ders (örn. Hücre Biyolojisi)", single = true)
                    Spacer(Modifier.height(6.dp))
                    Field(genSourceText, { genSourceText = it }, placeholder = "Ders metni / notların", minLines = 5)
                    Spacer(Modifier.height(8.dp))
                    Btn(if (generating) "Üretiliyor…" else "Kart Önerileri Al", {
                        if (!generating && genSourceText.isNotBlank()) {
                            generating = true
                            scope.launch {
                                genResult = gate.generateFlashcards(genSourceText, genSubject.ifBlank { "Genel" })
                                generating = false
                            }
                        }
                    }, enabled = !generating, emoji = "🤖")
                    Spacer(Modifier.height(8.dp))
                    AiResultView(genResult) { pairs ->
                        Column {
                            SectionLabel("öneriler", "✨")
                            pairs.forEach { (f, b) ->
                                Spacer(Modifier.height(6.dp))
                                Column(
                                    Modifier.fillMaxWidth().background(J.paper, RoundedCornerShape(10.dp)).padding(10.dp),
                                ) {
                                    Text(f, style = TitleM)
                                    Text(b, style = Small)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Btn("Tümünü Kart Olarak Ekle", {
                                scope.launch {
                                    pairs.forEach { (f, b) -> repo.createFlashcard(f, b) }
                                    genResult = null
                                    genSourceText = ""
                                    showGenForm = false
                                    refreshTick++
                                }
                            }, emoji = "✓")
                        }
                    }
                }
            }
        }

        item { SectionLabel("vadesi gelenler", "🔁") }
        if (due.isEmpty()) {
            item { Empty("🔁", "Vadesi gelen kart yok", "Yeni kart eklediğinde hemen vadesi gelir — ilk tekrar her zaman aynı gün.") }
        } else if (reviewIndex >= due.size) {
            item {
                Column(Modifier.fillMaxWidth().background(J.lime.copy(alpha = 0.25f), RoundedCornerShape(16.dp)).padding(16.dp)) {
                    Text("✓ Bugünlük tekrar bitti", style = TitleL)
                }
            }
        } else {
            val card = due[reviewIndex]
            val payload = runCatching { FlashcardPayload.fromJson(card.payload) }.getOrNull()
            item {
                Column(
                    Modifier.fillMaxWidth().background(J.card, RoundedCornerShape(16.dp)).padding(16.dp),
                ) {
                    Text("${reviewIndex + 1} / ${due.size}", style = Small.copy(color = J.inkFaint))
                    Text(payload?.front ?: card.title, style = TitleL)
                    if (revealed) {
                        Spacer(Modifier.height(10.dp))
                        Text(payload?.back ?: "", style = Small)
                    }
                }
            }
            if (!revealed) {
                item { Btn("Cevabı Göster", { revealed = true }) }
            } else {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        QualityBtn("Tekrar", 0, card.id, repo, scope) { reviewIndex++; revealed = false }
                        QualityBtn("Zor", 3, card.id, repo, scope) { reviewIndex++; revealed = false }
                        QualityBtn("İyi", 4, card.id, repo, scope) { reviewIndex++; revealed = false }
                        QualityBtn("Kolay", 5, card.id, repo, scope) { reviewIndex++; revealed = false }
                    }
                }
            }
        }
    }
}

@Composable
private fun QualityBtn(
    label: String,
    quality: Int,
    cardId: String,
    repo: LabRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    onDone: () -> Unit,
) {
    Btn(label, {
        scope.launch {
            repo.reviewFlashcard(cardId, quality)
            onDone()
        }
    })
}
