package com.beril.kaomoji.ui.lab2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.beril.kaomoji.lab.model.LearningSessionPayload
import com.beril.kaomoji.lab.model.ObjectKind
import com.beril.kaomoji.lab.repository.LabRepository
import com.beril.kaomoji.ui.Btn
import com.beril.kaomoji.ui.Display
import com.beril.kaomoji.ui.GhostBtn
import com.beril.kaomoji.ui.J
import com.beril.kaomoji.ui.Mono
import com.beril.kaomoji.ui.SectionLabel
import com.beril.kaomoji.ui.Small
import com.beril.kaomoji.ui.StatTile
import kotlinx.coroutines.launch

/**
 * AI destekli durum değerlendirmesi (§26/§27) — AI çıktısı asla tek gerçek kaynak değil:
 * önce gerçek sayılar (kavram/oturum/tamamlanma) gösterilir, AI yorumu bunun ÜZERİNE,
 * ayrı ve açıkça etiketli bir blok olarak gelir. "Harika gidiyorsun!" tarzı motivasyonel
 * metin değil, veri odaklı bir özet istenir (prompt bunu GroqAIProvider/GeminiAIProvider'da
 * zaten zorunlu kılıyor).
 */
@Composable
fun LabEvaluationScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { LabRepository(ctx) }
    val gate = remember { AICapabilityGate.forContext(ctx) }
    val scope = rememberCoroutineScope()

    var conceptCount by remember { mutableStateOf(0) }
    var sessionCount by remember { mutableStateOf(0) }
    var completedCount by remember { mutableStateOf(0) }
    var neglected by remember { mutableStateOf(0) }
    var loaded by remember { mutableStateOf(false) }
    var evaluation by remember { mutableStateOf<AIResult<String>?>(null) }
    var evaluating by remember { mutableStateOf(false) }
    var plan by remember { mutableStateOf<AIResult<String>?>(null) }
    var planning by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        val concepts = repo.getByKind(ObjectKind.CONCEPT)
        val sessions = repo.getByKind(ObjectKind.LEARNING_SESSION)
        val completedStates = sessions.map { runCatching { LearningSessionPayload.fromJson(it.payload) }.getOrNull() }
        conceptCount = concepts.size
        sessionCount = sessions.size
        completedCount = completedStates.count { it?.completed == true }
        val touchedConceptIds = sessions.flatMap { repo.relationshipsOf(it.id) }.map { it.toId }.toSet()
        neglected = concepts.count { it.id !in touchedConceptIds }
        loaded = true
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, 12.dp, 14.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            GhostBtn("Geri", onBack, emoji = "←")
            Spacer(Modifier.height(10.dp))
            Text("🪞 Durumu Değerlendir", style = Display)
            Text("Önce gerçek sayılar, AI yorumu bunun üzerine ayrı bir blok.", style = Small)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatTile("$conceptCount", "kavram", "🧠", J.forest)
                StatTile("$sessionCount", "oturum", "▶", J.apple)
                StatTile("$completedCount", "tamamlandı", "✓", J.forest)
            }
        }

        item {
            SectionLabel("gözden kaçanlar", "👀")
            Text(
                if (!loaded) "Hesaplanıyor…"
                else if (conceptCount == 0) "Henüz hiç kavram yok."
                else "$neglected / $conceptCount kavramla hiç öğrenme oturumu başlatılmadı.",
                style = Small,
            )
        }

        item {
            SectionLabel("ai değerlendirmesi", "🤖")
            Btn(if (evaluating) "İsteniyor…" else "Değerlendirmeyi İste", {
                if (!evaluating && loaded) {
                    evaluating = true
                    scope.launch {
                        val summary = buildString {
                            append("Toplam kavram: $conceptCount. ")
                            append("Başlatılan öğrenme oturumu: $sessionCount, tamamlanan: $completedCount. ")
                            append("Hiç çalışılmamış kavram sayısı: $neglected.")
                        }
                        evaluation = gate.evaluateProgress(summary)
                        evaluating = false
                    }
                }
            }, enabled = !evaluating && loaded, emoji = "🤖")
            Spacer(Modifier.height(6.dp))
            AiResultView(evaluation) { text -> Text(text, style = Mono) }
        }

        item {
            SectionLabel("plan önerisi", "🗓️")
            Text(
                "Sadece ÖNERİ üretir — hiçbir zaman otomatik uygulanmaz (§4 Mod C). " +
                    "Herhangi bir tarihi/hedefi değiştirmek için bunu kendin, elle yapman gerekir.",
                style = Small,
            )
            Spacer(Modifier.height(6.dp))
            Btn(if (planning) "İsteniyor…" else "7 Günlük Plan Öner", {
                if (!planning && loaded) {
                    planning = true
                    scope.launch {
                        val summary = buildString {
                            append("Toplam kavram: $conceptCount. ")
                            append("Başlatılan öğrenme oturumu: $sessionCount, tamamlanan: $completedCount. ")
                            append("Hiç çalışılmamış kavram sayısı: $neglected.")
                        }
                        plan = gate.proposeStudyPlan(summary, horizonDays = 7)
                        planning = false
                    }
                }
            }, enabled = !planning && loaded, emoji = "🗓️")
            Spacer(Modifier.height(6.dp))
            AiResultView(plan) { text -> Text(text, style = Mono) }
        }
    }
}
