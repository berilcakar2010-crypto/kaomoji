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
import com.beril.kaomoji.ai.ApiKeyStore
import com.beril.kaomoji.ai.GroqClient
import com.beril.kaomoji.data.Flashcard
import com.beril.kaomoji.data.Store
import com.beril.kaomoji.data.uid
import com.beril.kaomoji.storage.FileVault
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

    var apiKey by remember { mutableStateOf(ApiKeyStore.get(ctx) ?: "") }
    var showKeyField by remember { mutableStateOf(ApiKeyStore.get(ctx).isNullOrBlank()) }
    var generating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var lastExportPath by remember { mutableStateOf<String?>(null) }
    var flippedId by remember { mutableStateOf<String?>(null) }

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
                "Otomatik üret, tekrar et, Anki'ye aktar.",
                style = Small
            )
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
                        if (ApiKeyStore.get(ctx).isNullOrBlank()) "🔑 Groq API anahtarı gerekli"
                        else "🔑 Groq API anahtarı kayıtlı",
                        style = TitleM, modifier = Modifier.weight(1f)
                    )
                    Text(if (showKeyField) "▲" else "▼", style = Small)
                }
                if (showKeyField) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "console.groq.com üzerinden ücretsiz — FocusLock'ta kullandığın anahtarla aynısını kullanabilirsin.",
                        style = Tiny
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("gsk_...") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        colors = TextFieldDefaults.colors()
                    )
                    Spacer(Modifier.height(6.dp))
                    Btn("Kaydet", {
                        ApiKeyStore.set(ctx, apiKey)
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
                        val key = ApiKeyStore.get(ctx)
                        if (u == null) {
                            error = "Açık bir birim yok."
                        } else if (key.isNullOrBlank()) {
                            error = "Önce yukarıdan Groq API anahtarını kaydet."
                            showKeyField = true
                        } else {
                            error = null
                            generating = true
                            val sourceText = u.tasks.joinToString("\n") { it.text }
                            val subjName = c.subject(u.tasks.firstOrNull()?.subject ?: "")?.name ?: u.title
                            scope.launch {
                                try {
                                    val pairs = withContext(Dispatchers.IO) {
                                        GroqClient.generateFlashcards(key, sourceText, subjName, 8)
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
                    .clickable { flippedId = if (flipped) null else f.id }
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
