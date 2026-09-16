package com.beril.kaomoji.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beril.kaomoji.ai.AiClient
import com.beril.kaomoji.ai.AiProvider
import com.beril.kaomoji.ai.ApiKeyStore
import com.beril.kaomoji.data.Curriculum
import com.beril.kaomoji.data.CurriculumUnit
import com.beril.kaomoji.data.Flashcard
import com.beril.kaomoji.data.Store
import com.beril.kaomoji.data.uid
import com.beril.kaomoji.storage.FileVault
import com.beril.kaomoji.ui.nav.dpadFocusable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FlashcardsScreen(store: Store, vault: FileVault, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val c = store.curriculum
    val unit = store.currentUnit

    var provider by remember { mutableStateOf(ApiKeyStore.provider(ctx)) }
    var apiKey by remember(provider) { mutableStateOf(ApiKeyStore.get(ctx, provider) ?: "") }
    var showKeyField by remember(provider) { mutableStateOf(ApiKeyStore.get(ctx, provider).isNullOrBlank()) }
    var generating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var lastExportPath by remember { mutableStateOf<String?>(null) }
    var flippedId by remember { mutableStateOf<String?>(null) }
    var reviewing by remember { mutableStateOf(false) }
    var addingManual by remember { mutableStateOf(false) }

    if (reviewing) {
        SpacedRepetitionReview(store, onDone = { reviewing = false })
        return
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, 12.dp, 14.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            GhostBtn("Geri", onBack, emoji = "←")
            Spacer(Modifier.height(10.dp))
            Text("🃏 Kartlar (Anki)", style = Display)
            Text(
                "Manuel ekle, otomatik üret, aralıklı tekrarla (SM-2), Anki'ye aktar.",
                style = Small
            )
        }

        // ── aralıklı tekrar (SM-2) özeti ──
        item {
            val due = store.dueFlashcards().size
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(J.lilac.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                    .border(1.dp, J.lilac.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Text("🔁 Aralıklı Tekrar (SM-2)", style = TitleM)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (due > 0) "$due kart gösterim için bekliyor." else "Gösterim zamanı gelen kart yok.",
                    style = Small
                )
                Spacer(Modifier.height(8.dp))
                Btn(
                    if (due > 0) "Tekrara başla" else "Tekrar yok",
                    { reviewing = true },
                    bg = J.lilac,
                    enabled = due > 0,
                    emoji = "🔁"
                )
            }
        }

        // ── manuel kart ekleme ──
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(J.card, RoundedCornerShape(14.dp))
                    .border(1.dp, J.lineSoft, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().dpadFocusable(onClick = { addingManual = !addingManual }),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✍️ Kartı elle ekle (aktif hatırlatma)", style = TitleM, modifier = Modifier.weight(1f))
                    Text(if (addingManual) "▲" else "▼", style = Small)
                }
                if (addingManual) {
                    Spacer(Modifier.height(8.dp))
                    ManualCardForm(c, unit) { front, back, subject ->
                        store.addFlashcard(
                            Flashcard(
                                id = uid(), front = front, back = back, subject = subject,
                                unitId = unit?.id, createdAt = System.currentTimeMillis(), source = "manual"
                            )
                        )
                        addingManual = false
                    }
                }
            }
        }

        // ── API anahtarı ──
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(J.card, RoundedCornerShape(14.dp))
                    .border(1.dp, J.lineSoft, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().clickable { showKeyField = !showKeyField },
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
                            GhostBtn(
                                p.label,
                                {
                                    provider = p
                                    ApiKeyStore.setProvider(ctx, p)
                                },
                                emoji = if (p == provider) "●" else "○"
                            )
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
                    Btn("Kaydet", {
                        ApiKeyStore.set(ctx, apiKey, provider)
                        showKeyField = false
                    }, bg = J.forest, emoji = "✓")
                }
            }
        }

        // ── otomatik üretim ──
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(J.blush.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                    .dashed(J.blush.copy(alpha = 0.6f))
                    .padding(12.dp)
            ) {
                Text("Bu birimden otomatik kart üret", style = TitleM)
                Spacer(Modifier.height(4.dp))
                Text(unit?.title ?: "Şu an açık birim yok", style = Small)
                Spacer(Modifier.height(8.dp))
                if (error != null) {
                    Text("⚠️ $error", style = Tiny.copy(color = J.cherry))
                    Spacer(Modifier.height(6.dp))
                }
                Btn(
                    if (generating) "Üretiliyor…" else "🃏 8 kart üret",
                    {
                        val u = unit
                        val key = ApiKeyStore.get(ctx, provider)
                        if (u == null) {
                            error = "Açık bir birim yok."
                        } else if (key.isNullOrBlank()) {
                            error = "Önce yukarıdan ${provider.label} API anahtarını kaydet."
                            showKeyField = true
                        } else {
                            error = null
                            generating = true
                            val sourceText = u.tasks.joinToString("\n") { it.text }
                            val subjName = c.subject(u.tasks.firstOrNull()?.subject ?: "")?.name ?: u.title
                            scope.launch {
                                try {
                                    val pairs = withContext(Dispatchers.IO) {
                                        AiClient.generateFlashcards(ctx, key, sourceText, subjName, 8)
                                    }
                                    if (pairs.isEmpty()) {
                                        error = "Model kart üretemedi, tekrar dene."
                                    } else {
                                        val cards = pairs.map { (q, a) ->
                                            Flashcard(
                                                id = uid(), front = q, back = a,
                                                subject = u.tasks.firstOrNull()?.subject ?: "phys",
                                                unitId = u.id, createdAt = System.currentTimeMillis(),
                                                source = "auto"
                                            )
                                        }
                                        store.addFlashcards(cards)
                                    }
                                } catch (e: Exception) {
                                    error = e.message ?: "Bilinmeyen hata"
                                } finally {
                                    generating = false
                                }
                            }
                        }
                    },
                    enabled = !generating,
                    bg = J.cherry
                )
            }
        }

        // ── dışa aktarma ──
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(J.card, RoundedCornerShape(14.dp))
                    .border(1.dp, J.lineSoft, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Text("Anki'ye aktar", style = TitleM)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${store.flashcards.size} kart → sekme ile ayrılmış .txt dosyası. " +
                        "Anki'de: Dosya → İçe Aktar, alan ayracı olarak Tab seç.",
                    style = Tiny
                )
                Spacer(Modifier.height(8.dp))
                Btn("📤 .txt olarak dışa aktar", {
                    val tsv = store.flashcards.joinToString("\n") { f ->
                        "${f.front.replace("\t", " ").replace("\n", " ")}\t${f.back.replace("\t", " ").replace("\n", " ")}"
                    }
                    val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                    val uriRes = vault.writeText("Exports", "kaomoji_anki_$stamp.txt", "text/plain", tsv)
                    lastExportPath = uriRes?.toString() ?: "kaydedilemedi"
                }, bg = J.forest, enabled = store.flashcards.isNotEmpty())
                lastExportPath?.let {
                    Spacer(Modifier.height(6.dp))
                    Text("✓ Kaydedildi: Exports klasörü", style = Tiny.copy(color = J.forest))
                }
            }
        }

        // ── kart listesi ──
        item {
            Spacer(Modifier.height(4.dp))
            SectionLabel("kartların (${store.flashcards.size})", "🃏")
        }
        items(store.flashcards, key = { it.id }) { f ->
            val flipped = f.id == flippedId
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        if (f.source == "auto") J.lime.copy(alpha = 0.15f) else J.card,
                        RoundedCornerShape(14.dp)
                    )
                    .border(1.dp, J.lineSoft, RoundedCornerShape(14.dp))
                    .dpadFocusable(onClick = { flippedId = if (flipped) null else f.id })
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        c.subject(f.subject)?.emoji ?: "⚗️",
                        style = TextStyle(fontSize = 14.sp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (flipped) f.back else f.front,
                        style = Body, modifier = Modifier.weight(1f)
                    )
                    Text(
                        if (flipped) "cevap" else "soru — dokun",
                        style = Tiny.copy(color = J.inkFaint)
                    )
                }
            }
        }
    }
}

/** Elle kart oluşturma — otomatik üretime alternatif, aktif hatırlamayı kendi cümleleriyle kurmak isteyenler için. */
@Composable
private fun ManualCardForm(c: Curriculum, unit: CurriculumUnit?, onSave: (front: String, back: String, subject: String) -> Unit) {
    var front by remember { mutableStateOf("") }
    var back by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf(unit?.tasks?.firstOrNull()?.subject ?: c.subjects.firstOrNull()?.code ?: "") }

    Text("Soru", style = Tiny.copy(fontWeight = FontWeight.Bold))
    Spacer(Modifier.height(4.dp))
    Field(front, { front = it }, "Soru / ön yüz")
    Spacer(Modifier.height(8.dp))
    Text("Cevap", style = Tiny.copy(fontWeight = FontWeight.Bold))
    Spacer(Modifier.height(4.dp))
    Field(back, { back = it }, "Cevap / arka yüz")
    Spacer(Modifier.height(8.dp))
    Text("Ders", style = Tiny.copy(fontWeight = FontWeight.Bold))
    Spacer(Modifier.height(4.dp))
    Selector(c.subjects.map { it.code }, subject, { subject = it }) { code ->
        c.subject(code)?.let { "${it.emoji} ${it.name}" } ?: code
    }
    Spacer(Modifier.height(8.dp))
    Btn("Kartı kaydet", {
        if (front.isNotBlank() && back.isNotBlank()) {
            onSave(front.trim(), back.trim(), subject)
        }
    }, bg = J.forest, emoji = "✓")
}

/**
 * SM-2 tekrar akışı: gösterim zamanı gelen kartlar tek tek soru → cevap →
 * zorluk puanı sırasıyla gözden geçirilir. Her puandan sonra [Store.reviewFlashcard]
 * sonraki gösterim tarihini yeniden hesaplar (bkz. data/SM2.kt).
 */
@Composable
private fun SpacedRepetitionReview(store: Store, onDone: () -> Unit) {
    val due = remember { store.dueFlashcards() }
    var index by remember { mutableStateOf(0) }
    var showBack by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(14.dp, 12.dp)) {
        GhostBtn("Bitir", onDone, emoji = "←")
        Spacer(Modifier.height(10.dp))

        if (index >= due.size) {
            Text("🔁 Tekrar Tamamlandı", style = Display)
            Spacer(Modifier.height(6.dp))
            Text("${due.size} kart değerlendirildi.", style = Small)
            Spacer(Modifier.height(14.dp))
            Btn("Kapat", onDone, bg = J.forest, emoji = "✓")
            return@Column
        }

        val kart = due[index]
        Text("${index + 1} / ${due.size}", style = Mono)
        Spacer(Modifier.height(10.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .background(J.card, RoundedCornerShape(16.dp))
                .border(1.dp, J.lilac.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Text(kart.front, style = TitleL)
            if (showBack) {
                Spacer(Modifier.height(12.dp))
                Text("—", style = Tiny.copy(color = J.inkFaint))
                Spacer(Modifier.height(12.dp))
                Text(kart.back, style = Body)
            }
        }

        Spacer(Modifier.height(14.dp))

        if (!showBack) {
            Btn("Cevabı göster", { showBack = true }, bg = J.lilac, emoji = "👁️")
        } else {
            fun sonraki(kalite: Int) {
                store.reviewFlashcard(kart.id, kalite)
                showBack = false
                index++
            }
            Column {
                Btn("Hatırlamadım — tekrar", { sonraki(0) }, Modifier.fillMaxWidth(), bg = J.cherry)
                Spacer(Modifier.height(6.dp))
                Btn("Zor hatırladım", { sonraki(3) }, Modifier.fillMaxWidth(), bg = J.butter, fg = androidx.compose.ui.graphics.Color(0xFF1A0E05))
                Spacer(Modifier.height(6.dp))
                Btn("Hatırladım", { sonraki(4) }, Modifier.fillMaxWidth(), bg = J.apple)
                Spacer(Modifier.height(6.dp))
                Btn("Kolayca hatırladım", { sonraki(5) }, Modifier.fillMaxWidth(), bg = J.forest)
            }
        }
    }
}
