package com.beril.kaomoji.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beril.kaomoji.ai.AiClient
import com.beril.kaomoji.ai.ApiKeyStore
import com.beril.kaomoji.audio.Player
import com.beril.kaomoji.audio.Recorder
import com.beril.kaomoji.audio.fmtDuration
import com.beril.kaomoji.data.Recording
import com.beril.kaomoji.data.Store
import com.beril.kaomoji.data.uid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class RecordRequest(
    val prompt: String? = null,
    val unitId: String? = null,
    val taskId: String? = null,
    val isFeynman: Boolean = false,
    val projectId: String? = null
)

/**
 * Recording is two steps: record immediately, ask questions afterwards.
 * Nothing blocks the microphone.
 */
@Composable
fun RecordScreen(
    store: Store,
    recorder: Recorder,
    request: RecordRequest,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val c = store.curriculum
    var stage by remember { mutableStateOf(if (hasPermission) "ready" else "perm") }
    var elapsed by remember { mutableLongStateOf(0L) }
    var savedDuration by remember { mutableLongStateOf(0L) }
    var savedUri by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    // metadata
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf(request.unitId?.let { u ->
        c.allUnits.firstOrNull { it.id == u }?.tasks?.firstOrNull()?.subject
    } ?: "phys") }
    var lang by remember { mutableStateOf(if (request.isFeynman) "EN" else "TR") }
    var projectId by remember { mutableStateOf(request.projectId) }
    var needsReview by remember { mutableStateOf(false) }

    LaunchedEffect(hasPermission) { if (hasPermission && stage == "perm") stage = "ready" }

    LaunchedEffect(stage) {
        while (stage == "recording") {
            elapsed = recorder.elapsedMs()
            delay(200)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(J.paper)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (stage == "meta") "💾 Anlatımı kaydet" else "🎙️ Anlat",
                style = TitleL, modifier = Modifier.weight(1f)
            )
            if (stage != "recording") Text(
                "✕", style = TextStyle(fontSize = 19.sp, color = J.inkSoft),
                modifier = Modifier.clickable { recorder.cancel(); onCancel() }.padding(6.dp)
            )
        }

        Spacer(Modifier.height(14.dp))

        if (request.prompt != null && stage != "meta") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(J.lilac.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                    .dashed(J.lilac.copy(alpha = 0.5f), radius = 14f)
                    .padding(13.dp)
            ) {
                Text(
                    if (request.isFeynman) "FEYNMAN GÖREVİ" else "KONU",
                    style = Tiny.copy(color = J.lilac, fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(5.dp))
                Text(request.prompt, style = Body)
            }
            Spacer(Modifier.height(16.dp))
        }

        when (stage) {
            "perm" -> {
                Empty("🎤", "Mikrofon izni gerekli", "Kendi sesinle anlatmak bu uygulamanın kalbi.")
                Btn("İzin ver", onRequestPermission, emoji = "🎤")
            }

            "ready" -> {
                Column(
                    Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "● KAYDA HAZIR",
                        style = TextStyle(
                            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = J.cherry,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("dinliyor…", style = Small)
                    Spacer(Modifier.height(26.dp))
                    Box(
                        Modifier
                            .size(96.dp)
                            .background(J.cherry, RoundedCornerShape(50))
                            .clickable {
                                val vault = com.beril.kaomoji.storage.FileVault(
                                    storeCtx!!, store.storageUri
                                )
                                val name = "kaydi_${System.currentTimeMillis()}"
                                if (recorder.start(vault, name)) {
                                    stage = "recording"; error = null
                                } else error = "Kayıt başlatılamadı. Depolama klasörünü kontrol et."
                            },
                        contentAlignment = Alignment.Center
                    ) { Text("🎙️", style = TextStyle(fontSize = 38.sp)) }
                    Spacer(Modifier.height(14.dp))
                    Text("Dokun, hemen başla", style = Small)
                    if (error != null) {
                        Spacer(Modifier.height(12.dp))
                        Text(error!!, style = Small.copy(color = J.cherry))
                    }
                }
            }

            "recording" -> {
                Column(
                    Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Wave(recorder.isPaused)
                    Spacer(Modifier.height(18.dp))
                    Text(
                        fmtDuration(elapsed),
                        style = TextStyle(
                            fontSize = 40.sp, fontWeight = FontWeight.Bold,
                            color = J.cherry,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(if (recorder.isPaused) "duraklatıldı" else "kaydediyor…", style = Small)
                    Spacer(Modifier.height(30.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (recorder.isPaused)
                            Btn("Devam", { recorder.resume() }, bg = J.forest, emoji = "▶")
                        else
                            GhostBtn("Duraklat", { recorder.pause() }, emoji = "⏸")
                        Btn("Bitir", {
                            savedDuration = recorder.stop()
                            savedUri = recorder.outputUri
                            title = request.prompt?.take(48) ?: ""
                            stage = "meta"
                        }, bg = J.cherry, emoji = "⏹")
                    }
                }
            }

            "meta" -> {
                LazyColumn(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Sticker("🎙️", 40, J.cherry.copy(alpha = 0.18f))
                            Spacer(Modifier.width(11.dp))
                            Column {
                                Text(fmtDuration(savedDuration), style = TitleL)
                                Text("kaydedildi", style = Small)
                            }
                        }
                    }
                    item {
                        Text("Ne anlattın?", style = TitleM)
                        Spacer(Modifier.height(6.dp))
                        Field(title, { title = it }, "Başlık", single = true)
                    }
                    item {
                        Text("Ders", style = Small)
                        Spacer(Modifier.height(5.dp))
                        Selector(
                            c.subjects.map { it.code }, subject, { subject = it },
                            labels = { code -> c.subject(code)?.let { "${it.emoji} ${it.name}" } ?: code }
                        )
                    }
                    item {
                        Text("Dil", style = Small)
                        Spacer(Modifier.height(5.dp))
                        Selector(listOf("EN", "TR", "JP", "DE"), lang, { lang = it })
                    }
                    item {
                        Text("Proje (opsiyonel)", style = Small)
                        Spacer(Modifier.height(5.dp))
                        Selector(
                            listOf("—") + c.projects.map { it.id },
                            projectId ?: "—",
                            { projectId = if (it == "—") null else it },
                            labels = { id ->
                                if (id == "—") "yok"
                                else c.projects.firstOrNull { it.id == id }
                                    ?.let { "${it.emoji} ${it.name}" } ?: id
                            }
                        )
                    }
                    item {
                        Row(
                            Modifier.clickable { needsReview = !needsReview },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(needsReview, { needsReview = !needsReview }, J.butter)
                            Spacer(Modifier.width(9.dp))
                            Text("Gözden geçirilmeli", style = Body)
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Btn("Kaydet", {
                                store.addRecording(
                                    Recording(
                                        id = uid(),
                                        title = title.ifBlank { "Adsız anlatım" },
                                        uri = savedUri ?: "",
                                        durationMs = savedDuration,
                                        createdAt = System.currentTimeMillis(),
                                        subject = subject,
                                        unitId = request.unitId,
                                        taskId = request.taskId,
                                        projectId = projectId,
                                        language = lang,
                                        needsReview = needsReview,
                                        isFeynman = request.isFeynman
                                    )
                                )
                                request.taskId?.let { if (store.done[it] != true) store.toggleTask(it) }
                                onDone()
                            }, Modifier.weight(1f), J.forest, emoji = "💾")
                            GhostBtn("Sonra düzenle", {
                                store.addRecording(
                                    Recording(
                                        id = uid(),
                                        title = "Adsız anlatım",
                                        uri = savedUri ?: "",
                                        durationMs = savedDuration,
                                        createdAt = System.currentTimeMillis(),
                                        unitId = request.unitId,
                                        taskId = request.taskId,
                                        isFeynman = request.isFeynman
                                    )
                                )
                                onDone()
                            })
                        }
                    }
                }
            }
        }
    }
}

/** Set once from MainActivity so the record button can build a FileVault. */
var storeCtx: android.content.Context? = null

@Composable
private fun Wave(paused: Boolean) {
    var t by remember { mutableIntStateOf(0) }
    LaunchedEffect(paused) {
        while (!paused) { t++; delay(110) }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(11) { i ->
            val h = if (paused) 10 else
                (12 + (kotlin.math.sin((t + i * 2) * 0.7) * 22).toInt().let { kotlin.math.abs(it) })
            Box(
                Modifier
                    .width(5.dp)
                    .height(h.dp)
                    .background(
                        if (paused) J.line else J.cherry.copy(alpha = 0.75f),
                        RoundedCornerShape(50)
                    )
            )
        }
    }
}

// ── LIBRARY ────────────────────────────────────────────────────────

@Composable
fun AudioLibraryScreen(
    store: Store,
    player: Player,
    onBack: () -> Unit,
    onRecord: () -> Unit
) {
    val c = store.curriculum
    var filter by remember { mutableStateOf("Tümü") }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Recording?>(null) }

    LaunchedEffect(player.isPlaying) {
        while (player.isPlaying) { player.tick(); delay(300) }
    }

    val filters = listOf("Tümü", "Feynman", "Favori", "Gözden geçir", "EN", "Proje")
    val list = store.recordings.filter { r ->
        val f = when (filter) {
            "Feynman" -> r.isFeynman
            "Favori" -> r.favorite
            "Gözden geçir" -> r.needsReview
            "EN" -> r.language == "EN"
            "Proje" -> r.projectId != null
            else -> true
        }
        f && (query.isBlank() || r.title.contains(query, true))
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, 12.dp, 14.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            GhostBtn("Geri", onBack, emoji = "←")
            Spacer(Modifier.height(10.dp))
            Text("🎙️ Anlatımlarım", style = Display)
            Text("Kendi sesinle kurduğun bilgi arşivi", style = Small)
        }
        item { Field(query, { query = it }, "Ara…", single = true) }
        item { Selector(filters, filter, { filter = it }) }
        item {
            Btn("Yeni anlatım", onRecord, bg = J.cherry, emoji = "🎙️")
        }

        if (list.isEmpty()) {
            item { Empty("📼", "Kayıt yok", "Anlatmadığın şeyi bilmiyorsundur.") }
        }

        items(list) { r ->
            CassetteCard(r, c, player, store) { editing = r }
        }
    }

    editing?.let { rec ->
        androidx.compose.ui.window.Dialog(onDismissRequest = { editing = null }) {
            var t by remember { mutableStateOf(rec.title) }
            var subj by remember { mutableStateOf(rec.subject) }
            var lang by remember { mutableStateOf(rec.language) }
            var proj by remember { mutableStateOf(rec.projectId) }
            var review by remember { mutableStateOf(rec.needsReview) }
            Sheet("Anlatımı düzenle", { editing = null }) {
                Field(t, { t = it }, "Başlık", single = true)
                Spacer(Modifier.height(10.dp))
                Text("Ders", style = Small)
                Spacer(Modifier.height(5.dp))
                Selector(
                    listOf("—") + c.subjects.map { it.code },
                    subj ?: "—",
                    { subj = if (it == "—") null else it },
                    labels = { code -> if (code == "—") "yok" else c.subject(code)?.let { "${it.emoji} ${it.name}" } ?: code }
                )
                Spacer(Modifier.height(10.dp))
                Text("Dil", style = Small)
                Spacer(Modifier.height(5.dp))
                Selector(listOf("EN", "TR", "JP", "DE"), lang, { lang = it })
                Spacer(Modifier.height(10.dp))
                Text("Proje", style = Small)
                Spacer(Modifier.height(5.dp))
                Selector(
                    listOf("—") + c.projects.map { it.id },
                    proj ?: "—",
                    { proj = if (it == "—") null else it },
                    labels = { id ->
                        if (id == "—") "yok"
                        else c.projects.firstOrNull { it.id == id }?.let { "${it.emoji} ${it.name}" } ?: id
                    }
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.clickable { review = !review },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(review, { review = !review }, J.butter)
                    Spacer(Modifier.width(9.dp))
                    Text("Gözden geçirilmeli", style = Body)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Btn("Kaydet", {
                        store.updateRecording(
                            rec.copy(title = t, subject = subj, language = lang, projectId = proj, needsReview = review)
                        )
                        editing = null
                    }, Modifier.weight(1f), J.forest)
                    GhostBtn("Sil", {
                        store.deleteRecording(rec.id); editing = null
                    }, color = J.cherry)
                }
            }
        }
    }
}

@Composable
private fun CassetteCard(
    r: Recording,
    c: com.beril.kaomoji.data.Curriculum,
    player: Player,
    store: Store,
    onEdit: () -> Unit
) {
    val playing = player.currentId == r.id && player.isPlaying
    val active = player.currentId == r.id
    val unitTitle = r.unitId?.let { u -> c.allUnits.firstOrNull { it.id == u }?.title }
    val sd = r.subject?.let { c.subject(it) }

    Column(
        Modifier
            .fillMaxWidth()
            .background(J.card, RoundedCornerShape(16.dp))
            .border(
                if (active) 2.dp else 1.dp,
                if (active) J.cherry.copy(alpha = 0.5f) else J.line,
                RoundedCornerShape(16.dp)
            )
            .padding(13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(42.dp)
                    .background(
                        if (playing) J.cherry else J.paperDeep,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        if (r.uri.isNotBlank()) player.toggle(r.id, r.uri, r.lastPositionMs)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (playing) "⏸" else "▶",
                    style = TextStyle(fontSize = 16.sp, color = if (playing) Color.White else J.ink)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(r.title, style = TitleM, maxLines = 2)
                Spacer(Modifier.height(3.dp))
                Text(
                    buildString {
                        append(fmtDuration(r.durationMs))
                        append(" · ").append(r.language)
                        unitTitle?.let { append(" · ").append(it) }
                    },
                    style = Tiny, maxLines = 1
                )
            }
            Text(
                "⋯", style = TextStyle(fontSize = 19.sp, color = J.inkSoft),
                modifier = Modifier.clickable { onEdit() }.padding(6.dp)
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            if (r.isFeynman) Chip("🎓 Feynman", J.lilac)
            sd?.let { Chip("${it.emoji} ${it.name}", subjectColor(it.color)) }
            if (r.needsReview) Chip("gözden geçir", J.butter)
            Spacer(Modifier.weight(1f))
            Text(
                if (r.favorite) "★" else "☆",
                style = TextStyle(fontSize = 16.sp, color = if (r.favorite) J.butter else J.inkFaint),
                modifier = Modifier.clickable {
                    store.updateRecording(r.copy(favorite = !r.favorite))
                }
            )
        }

        if (active) {
            Spacer(Modifier.height(10.dp))
            val dur = player.durationMs.coerceAtLeast(1)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .pointerInput(r.id) {
                        detectTapGesturesX { fraction -> player.seekTo((dur * fraction).toLong()) }
                    },
                contentAlignment = Alignment.Center
            ) { Bar(player.positionMs.toFloat() / dur, J.cherry, 5) }
            Row {
                Text(fmtDuration(player.positionMs), style = Mono)
                Spacer(Modifier.weight(1f))
                listOf(1.0f, 1.25f, 1.5f).forEach { s ->
                    Text(
                        "${s}x",
                        style = Tiny.copy(
                            color = if (player.speed == s) J.cherry else J.inkFaint,
                            fontWeight = if (player.speed == s) FontWeight.Bold else FontWeight.Normal
                        ),
                        modifier = Modifier
                            .clickable { player.changeSpeed(s) }
                            .padding(horizontal = 5.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(fmtDuration(dur), style = Mono)
            }
        }

        // ── AI transkripsiyon + anlatım analizi ──
        TranscriptSection(r, store)
    }
}

/** Ses kaydını seçili sağlayıcıyla (Groq Whisper veya Gemini) yazıya döker, sonra kısa bir geri bildirim üretir.
 *  Transkript ve analiz her zaman yeniden üretilebilir; transkript elle de düzenlenebilir. */
@Composable
private fun TranscriptSection(r: Recording, store: Store) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var busyTranscribe by remember(r.id) { mutableStateOf(false) }
    var busyAnalyze by remember(r.id) { mutableStateOf(false) }
    var err by remember(r.id) { mutableStateOf<String?>(null) }
    var editingTranscript by remember(r.id) { mutableStateOf(false) }
    var draft by remember(r.id) { mutableStateOf(r.transcript ?: "") }

    fun runTranscribe() {
        val provider = ApiKeyStore.provider(ctx)
        val key = ApiKeyStore.get(ctx, provider)
        if (key.isNullOrBlank()) {
            err = "Önce Kartlar (Anki) ekranından ${provider.label} API anahtarını kaydet."
            return
        }
        if (r.uri.isBlank()) {
            err = "Ses dosyası bulunamadı."
            return
        }
        busyTranscribe = true
        err = null
        scope.launch {
            try {
                val tmp = withContext(Dispatchers.IO) {
                    val uri = android.net.Uri.parse(r.uri)
                    val input = if (uri.scheme == "content")
                        ctx.contentResolver.openInputStream(uri)
                    else File(uri.path ?: "").inputStream()
                    val f = File(ctx.cacheDir, "transcribe_${r.id}.m4a")
                    input?.use { ins -> f.outputStream().use { out -> ins.copyTo(out) } }
                    f
                }
                if (!tmp.exists() || tmp.length() == 0L) {
                    err = "Ses dosyası okunamadı ya da boş."
                    return@launch
                }
                val text = withContext(Dispatchers.IO) {
                    AiClient.transcribeAudio(ctx, key, tmp)
                }
                if (text.isBlank()) {
                    err = "Model boş bir transkript döndürdü, tekrar dene."
                } else {
                    // Yeni transkript eskisiyle uyuşmayacağı için önceki analiz de geçersiz.
                    store.updateRecording(r.copy(transcript = text, analysis = null))
                    draft = text
                }
            } catch (e: Exception) {
                err = e.message ?: "Transkripsiyon başarısız"
            } finally {
                busyTranscribe = false
            }
        }
    }

    fun runAnalyze() {
        if (busyAnalyze) return
        val provider = ApiKeyStore.provider(ctx)
        val key = ApiKeyStore.get(ctx, provider)
        if (key.isNullOrBlank()) {
            err = "Önce Kartlar (Anki) ekranından ${provider.label} API anahtarını kaydet."
            return
        }
        busyAnalyze = true
        err = null
        scope.launch {
            try {
                val analysis = withContext(Dispatchers.IO) {
                    AiClient.analyzeTranscript(ctx, key, r.transcript ?: "", r.title)
                }
                store.updateRecording(r.copy(analysis = analysis))
            } catch (e: Exception) {
                err = e.message ?: "Analiz başarısız"
            } finally {
                busyAnalyze = false
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    Column(
        Modifier
            .fillMaxWidth()
            .background(J.paperDeep, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        if (r.transcript.isNullOrBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (busyTranscribe) "📝 Transkribe ediliyor…" else "📝 Transkripsiyon yok",
                    style = Tiny, modifier = Modifier.weight(1f)
                )
                if (!busyTranscribe) {
                    GhostBtn("Transkribe et", { runTranscribe() })
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "📝 Transkripsiyon", style = Tiny.copy(fontWeight = FontWeight.Bold, color = J.inkSoft),
                    modifier = Modifier.weight(1f)
                )
                if (!busyTranscribe && !editingTranscript) {
                    Text(
                        "✏️", style = TextStyle(fontSize = 13.sp),
                        modifier = Modifier.clickable { draft = r.transcript ?: ""; editingTranscript = true }.padding(4.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "🔁", style = TextStyle(fontSize = 13.sp),
                        modifier = Modifier.clickable { runTranscribe() }.padding(4.dp)
                    )
                }
                if (busyTranscribe) Text("yenileniyor…", style = Tiny)
            }
            Spacer(Modifier.height(4.dp))
            if (editingTranscript) {
                Field(draft, { draft = it }, "Transkript")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Btn("Kaydet", {
                        store.updateRecording(r.copy(transcript = draft))
                        editingTranscript = false
                    }, bg = J.forest, emoji = "✓")
                    GhostBtn("Vazgeç", {
                        draft = r.transcript ?: ""
                        editingTranscript = false
                    })
                }
            } else {
                Text(r.transcript!!, style = Small)
            }

            Spacer(Modifier.height(8.dp))
            if (r.analysis.isNullOrBlank()) {
                GhostBtn(
                    if (busyAnalyze) "Analiz ediliyor…" else "🔎 Anlatımı analiz et",
                    { runAnalyze() }
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "🔎 Analiz", style = Tiny.copy(fontWeight = FontWeight.Bold, color = J.inkSoft),
                        modifier = Modifier.weight(1f)
                    )
                    if (!busyAnalyze) {
                        Text(
                            "🔁", style = TextStyle(fontSize = 13.sp),
                            modifier = Modifier.clickable { runAnalyze() }.padding(4.dp)
                        )
                    } else Text("yenileniyor…", style = Tiny)
                }
                Spacer(Modifier.height(4.dp))
                Text(r.analysis!!, style = Small)
            }
        }
        err?.let {
            Spacer(Modifier.height(6.dp))
            Text("⚠️ $it", style = Tiny.copy(color = J.cherry))
        }
    }
}

private suspend fun PointerInputScope.detectTapGesturesX(
    onTap: (Float) -> Unit
) {
    detectTapGestures { offset ->
        onTap((offset.x / size.width).coerceIn(0f, 1f))
    }
}
