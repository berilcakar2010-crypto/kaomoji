package com.beril.kaomoji.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.beril.kaomoji.ai.AiClient
import com.beril.kaomoji.ai.AiProvider
import com.beril.kaomoji.ai.ApiKeyStore
import com.beril.kaomoji.data.Store
import com.beril.kaomoji.ui.nav.dpadFocusable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * "Şu ana kadarki durumu değerlendir" — Store'daki ilerleme, seri, hata örüntüsü
 * ve tekrar kartı verilerinden bir özet çıkarır, AI'a (Groq/Gemini) gönderip
 * kısa, veriye dayalı bir değerlendirme ister. Yapay zekâ hiçbir zaman veri
 * kaynağı değildir — sadece var olan istatistiklerin yorumudur.
 */
@Composable
fun EvaluationScreen(store: Store, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var provider by remember { mutableStateOf(ApiKeyStore.provider(ctx)) }
    var apiKey by remember(provider) { mutableStateOf(ApiKeyStore.get(ctx, provider) ?: "") }
    var showKeyField by remember(provider) { mutableStateOf(ApiKeyStore.get(ctx, provider).isNullOrBlank()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<String?>(null) }

    val summary = remember { buildStatsSummary(store) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, 12.dp, 14.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            GhostBtn("Geri", onBack, emoji = "←")
            Spacer(Modifier.height(10.dp))
            Text("🤖 Durumu Değerlendir", style = Display)
            Text(
                "Mevcut istatistiklerin AI ile kısa, veriye dayalı bir değerlendirmesi.",
                style = Small
            )
        }

        item {
            Card(border = J.lineSoft) {
                Text("Gönderilecek özet", style = TitleM)
                Spacer(Modifier.height(6.dp))
                Text(summary, style = Tiny.copy(color = J.inkFaint))
            }
        }

        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(J.card, RoundedCornerShape(14.dp))
                    .border(1.dp, J.lineSoft, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().dpadFocusable(onClick = { showKeyField = !showKeyField }),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (ApiKeyStore.get(ctx, provider).isNullOrBlank()) "🔑 ${provider.label} API anahtarı gerekli"
                        else "🔑 ${provider.label} API anahtarı kayıtlı",
                        style = TitleM, modifier = Modifier.weight(1f)
                    )
                    Text(if (showKeyField) "▲" else "▼", style = Small)
                }
                if (showKeyField) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AiProvider.entries.forEach { p ->
                            GhostBtn(p.label, { provider = p; ApiKeyStore.setProvider(ctx, p) }, emoji = if (p == provider) "●" else "○")
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("${provider.keySource} — anahtarını gir ve kaydet.", style = Tiny)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(provider.keyHint) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        colors = TextFieldDefaults.colors()
                    )
                    Spacer(Modifier.height(6.dp))
                    Btn("Kaydet", { ApiKeyStore.set(ctx, apiKey, provider); showKeyField = false }, bg = J.forest, emoji = "✓")
                }
            }
        }

        item {
            if (error != null) {
                Text("⚠️ $error", style = Tiny.copy(color = J.cherry))
                Spacer(Modifier.height(6.dp))
            }
            Btn(
                if (loading) "Değerlendiriliyor…" else "🤖 Değerlendir",
                {
                    val key = ApiKeyStore.get(ctx, provider)
                    if (key.isNullOrBlank()) {
                        error = "Önce yukarıdan ${provider.label} API anahtarını kaydet."
                        showKeyField = true
                    } else {
                        error = null
                        loading = true
                        scope.launch {
                            try {
                                val eval = withContext(Dispatchers.IO) {
                                    AiClient.evaluateProgress(ctx, key, summary)
                                }
                                result = eval
                            } catch (e: Exception) {
                                error = e.message ?: "Bilinmeyen hata"
                            } finally {
                                loading = false
                            }
                        }
                    }
                },
                enabled = !loading,
                bg = J.cherry
            )
        }

        result?.let { r ->
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(J.mint.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
                        .border(1.dp, J.forest.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Text("Değerlendirme", style = TitleM)
                    Spacer(Modifier.height(6.dp))
                    Text(r, style = Body)
                }
            }
        }
    }
}

/** Store'daki verilerden AI'a gönderilecek düz metin özet — hiçbir istatistik uydurulmaz. */
private fun buildStatsSummary(store: Store): String = buildString {
    val c = store.curriculum
    val toplamGorev = c.totalTasks
    val tamamlanan = store.totalDone
    val yuzde = if (toplamGorev > 0) (tamamlanan * 100 / toplamGorev) else 0

    appendLine("İlerleme: $tamamlanan / $toplamGorev görev (%$yuzde)")
    store.currentUnit?.let { appendLine("Mevcut birim: ${it.title}") }
    appendLine("Güncel seri (streak): ${store.currentStreak} gün")
    appendLine("Çözülen problem: ${store.problemsSolved} / ${store.problemsAttempted} denenen")

    val patterns = store.mistakePatterns()
    if (patterns.isNotEmpty()) {
        appendLine("Tekrar eden hata örüntüleri:")
        patterns.forEach { (subject, count) ->
            appendLine("  - ${c.subject(subject)?.name ?: subject}: $count çözülmemiş hata")
        }
    } else {
        appendLine("Tekrar eden hata örüntüsü yok.")
    }

    val kartlar = store.flashcards
    if (kartlar.isNotEmpty()) {
        val ortalamaEf = kartlar.map { it.easeFactor }.average()
        val bekleyen = store.dueFlashcards().size
        appendLine("Tekrar kartları: ${kartlar.size} toplam, $bekleyen gösterim bekliyor, ortalama kolaylık faktörü ${"%.2f".format(ortalamaEf)}")
    } else {
        appendLine("Henüz tekrar kartı yok.")
    }

    appendLine("Haftalık değerlendirme sayısı: ${store.reviews.size}")
    appendLine("Anlatım (Feynman) sayısı: ${store.recordings.count { it.isFeynman }}")
}
