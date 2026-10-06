package com.beril.kaomoji.ui.lab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
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
import com.beril.kaomoji.lab.repository.LabRepository
import com.beril.kaomoji.lab.repository.StatsSnapshot
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
 * AI destekli durum değerlendirmesi + İstatistik bölümü (§26/§27). AI çıktısı asla tek gerçek
 * kaynak değil: önce gerçek sayılar (kavram/oturum/hata/kart/seri — `LabRepository.
 * statsSnapshot()`) gösterilir, AI yorumu bunun ÜZERİNE, ayrı ve açıkça etiketli bir blok olarak
 * gelir. AI'a giden özet ekranda gösterilen sayılarla AYNI `StatsSnapshot`'tan üretiliyor
 * (`toSummary()`) — ikisi asla birbirinden sapamaz, AI daha önce sadece kavram/oturum sayılarını
 * görüyordu, artık hatalar/kartlar/seri de dahil, gerçekten daha iyi bir değerlendirme için.
 */
@Composable
fun LabEvaluationScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { LabRepository(ctx) }
    val gate = remember { AICapabilityGate.forContext(ctx) }
    val scope = rememberCoroutineScope()

    var stats by remember { mutableStateOf<StatsSnapshot?>(null) }
    var evaluation by remember { mutableStateOf<AIResult<String>?>(null) }
    var evaluating by remember { mutableStateOf(false) }
    var plan by remember { mutableStateOf<AIResult<String>?>(null) }
    var planning by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { stats = repo.statsSnapshot() }

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

        val s = stats
        item { SectionLabel("istatistikler", "📊") }
        if (s == null) {
            item { Text("Hesaplanıyor…", style = Small) }
        } else {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatTile("${s.conceptCount}", "kavram", "🧠", J.forest)
                    StatTile("${s.sessionCount}", "oturum", "▶", J.apple)
                    StatTile("${s.completedSessionCount}", "tamamlandı", "✓", J.forest)
                }
            }
            item {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatTile("${s.mistakeCount}", "hata kaydı", "⚠️", J.cherry)
                    StatTile("${s.dueFlashcardCount}", "vadesi gelen kart", "🃏", J.butter)
                    StatTile("${s.streakDays}", "günlük seri", "🔥", J.blush)
                }
            }
            item {
                SectionLabel("gözden kaçanlar", "👀")
                Text(
                    if (s.conceptCount == 0) "Henüz hiç kavram yok."
                    else "${s.neglectedConceptCount} / ${s.conceptCount} kavramla hiç öğrenme oturumu başlatılmadı.",
                    style = Small,
                )
            }
            if (s.topMistakeCategories.isNotEmpty()) {
                item {
                    SectionLabel("en sık hata türleri", "🔁")
                    Text(
                        s.topMistakeCategories.joinToString("  ·  ") { (category, count) -> "$category ($count)" },
                        style = Small,
                    )
                }
            }
            if (s.flashcardCount > 0 && s.avgEaseFactor != null) {
                item {
                    SectionLabel("tekrar kartları", "🃏")
                    Text(
                        "${s.flashcardCount} kart, ortalama kolaylık katsayısı ${"%.2f".format(s.avgEaseFactor)} " +
                            "(SM-2'de 2.5 başlangıç değeri — düşükse kartlar zorlaşıyor, kaçırılıyor demektir).",
                        style = Small,
                    )
                }
            }
        }

        item {
            SectionLabel("ai değerlendirmesi", "🤖")
            Btn(if (evaluating) "İsteniyor…" else "Değerlendirmeyi İste", {
                if (!evaluating && s != null) {
                    evaluating = true
                    scope.launch {
                        evaluation = gate.evaluateProgress(s.toSummary())
                        evaluating = false
                    }
                }
            }, enabled = !evaluating && s != null, emoji = "🤖")
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
                if (!planning && s != null) {
                    planning = true
                    scope.launch {
                        plan = gate.proposeStudyPlan(s.toSummary(), horizonDays = 7)
                        planning = false
                    }
                }
            }, enabled = !planning && s != null, emoji = "🗓️")
            Spacer(Modifier.height(6.dp))
            AiResultView(plan) { text -> Text(text, style = Mono) }
        }
    }
}
