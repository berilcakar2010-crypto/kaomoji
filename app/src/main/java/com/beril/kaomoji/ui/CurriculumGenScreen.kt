package com.beril.kaomoji.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.beril.kaomoji.ai.AiClient
import com.beril.kaomoji.ai.AiProvider
import com.beril.kaomoji.ai.ApiKeyStore
import com.beril.kaomoji.data.CurriculumLoader
import com.beril.kaomoji.data.Store
import com.beril.kaomoji.storage.DocumentTextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Kullanıcının yüklediği bir .md/.txt/.pdf belgesini seçili AI sağlayıcısına
 * (Groq/Gemini) göndererek CurriculumLoader şemasına uygun, o belgeye özel
 * sıfırdan bir müfredat ürettirir. Üretilen JSON önce doğrulanır (CurriculumLoader.parse),
 * sonra filesDir'e özel müfredat olarak kaydedilir ve varsayılan assets/curriculum.json'ın
 * yerini alır. Uygulamanın onu yükleyebilmesi için (Store başlangıçta bir kere okuduğundan)
 * bir yeniden başlatma (Activity.recreate) gerekir.
 */
@Composable
fun CurriculumGenScreen(store: Store, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var provider by remember { mutableStateOf(ApiKeyStore.provider(ctx)) }
    var apiKey by remember(provider) { mutableStateOf(ApiKeyStore.get(ctx, provider) ?: "") }
    var showKeyField by remember(provider) { mutableStateOf(ApiKeyStore.get(ctx, provider).isNullOrBlank()) }

    var pickedName by remember { mutableStateOf<String?>(null) }
    var extracting by remember { mutableStateOf(false) }
    var extractedText by remember { mutableStateOf<String?>(null) }
    var generating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }
    var hasCustom by remember { mutableStateOf(CurriculumLoader.hasCustom(ctx)) }

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        pickedName = DocumentTextExtractor.displayName(ctx, uri)
        extractedText = null
        error = null
        success = false
        extracting = true
        scope.launch {
            try {
                val text = withContext(Dispatchers.IO) { DocumentTextExtractor.extract(ctx, uri) }
                extractedText = text
            } catch (e: Exception) {
                error = e.message ?: "Belge okunamadı"
            } finally {
                extracting = false
            }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, 12.dp, 14.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            GhostBtn("Geri", onBack, emoji = "←")
            Spacer(Modifier.height(10.dp))
            Text("🧬 Müfredat Oluştur", style = Display)
            Text(
                "Bir belge (.md veya .pdf) yükle, seçtiğin AI o belgeye göre sıfırdan bir müfredat kursun.",
                style = Small
            )
        }

        // ── API anahtarı (Kartlar/Anlatımlar ekranıyla aynı, paylaşılan anahtar) ──
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
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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
                                { provider = p; ApiKeyStore.setProvider(ctx, p) },
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

        // ── belge seçimi ──
        item {
            Card {
                Text("1. Belge seç", style = TitleM)
                Spacer(Modifier.height(4.dp))
                Text("Ders programı, çalışma planı, kitap içindekiler tablosu — ne olursa.", style = Tiny)
                Spacer(Modifier.height(8.dp))
                Btn(
                    pickedName ?: "📄 Dosya seç (.md / .pdf)",
                    { pickFile.launch(arrayOf("text/markdown", "text/plain", "application/pdf", "application/octet-stream")) },
                    bg = J.sky, emoji = if (pickedName != null) "📄" else null
                )
                if (extracting) {
                    Spacer(Modifier.height(6.dp))
                    Text("Metin çıkarılıyor…", style = Small)
                }
                extractedText?.let {
                    Spacer(Modifier.height(6.dp))
                    Text("✓ ${it.length} karakter okundu", style = Small.copy(color = J.forest))
                }
            }
        }

        // ── üretim ──
        item {
            Card(bg = J.blush.copy(alpha = 0.18f), border = J.blush.copy(alpha = 0.6f)) {
                Text("2. Müfredatı üret", style = TitleM)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Bu, mevcut müfredatın yerini alır. Devam eden görev/kayıt geçmişin silinmez ama " +
                        "yeni müfredattaki birimlerle otomatik eşleşmez — sıfırdan başlamış gibi olur.",
                    style = Tiny
                )
                Spacer(Modifier.height(8.dp))
                if (error != null) {
                    Text("⚠️ $error", style = Tiny.copy(color = J.cherry))
                    Spacer(Modifier.height(6.dp))
                }
                Btn(
                    if (generating) "Üretiliyor… (1-2 dk sürebilir)" else "🧬 Müfredatı Oluştur",
                    {
                        val key = ApiKeyStore.get(ctx, provider)
                        val text = extractedText
                        if (key.isNullOrBlank()) {
                            error = "Önce yukarıdan ${provider.label} API anahtarını kaydet."
                            showKeyField = true
                        } else if (text.isNullOrBlank()) {
                            error = "Önce bir belge seç."
                        } else {
                            error = null
                            success = false
                            generating = true
                            val title = pickedName ?: "Yüklenen belge"
                            scope.launch {
                                try {
                                    val raw = withContext(Dispatchers.IO) {
                                        AiClient.generateCurriculum(ctx, key, title, text)
                                    }
                                    withContext(Dispatchers.IO) { CurriculumLoader.saveCustom(ctx, raw) }
                                    success = true
                                    hasCustom = true
                                } catch (e: Exception) {
                                    error = e.message ?: "Müfredat üretilemedi. Model geçerli JSON döndürmemiş olabilir — tekrar dene."
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

        if (success) {
            item {
                Card(bg = J.mint.copy(alpha = 0.3f), border = J.forest.copy(alpha = 0.4f)) {
                    Text("✓ Yeni müfredat kaydedildi", style = TitleM)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Uygulamanın bunu yükleyebilmesi için bir yeniden başlatma gerekiyor.",
                        style = Small
                    )
                    Spacer(Modifier.height(8.dp))
                    Btn("🔄 Uygulamayı Yenile", {
                        (ctx as? Activity)?.recreate()
                    }, bg = J.forest)
                }
            }
        }

        if (hasCustom) {
            item {
                GhostBtn("Varsayılan müfredata dön", {
                    CurriculumLoader.clearCustom(ctx)
                    hasCustom = false
                    (ctx as? Activity)?.recreate()
                }, color = J.cherry)
            }
        }
    }
}
