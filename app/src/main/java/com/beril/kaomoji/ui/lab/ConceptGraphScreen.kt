package com.beril.kaomoji.ui.lab

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.ContextCompat
import com.beril.kaomoji.ai.engine.AICapabilityGate
import com.beril.kaomoji.ai.engine.AIResult
import com.beril.kaomoji.lab.audio.LabPlayer
import com.beril.kaomoji.lab.audio.LabRecorder
import com.beril.kaomoji.lab.audio.newVideoFile
import com.beril.kaomoji.lab.audio.videoFileUri
import com.beril.kaomoji.lab.graph.EdgeBucket
import com.beril.kaomoji.lab.graph.bucketRelationships
import com.beril.kaomoji.lab.model.ExplanationPayload
import com.beril.kaomoji.lab.model.KnowledgeObjectEntity
import com.beril.kaomoji.lab.repository.LabRepository
import kotlinx.coroutines.launch
import com.beril.kaomoji.ui.Btn
import com.beril.kaomoji.ui.Display
import com.beril.kaomoji.ui.Empty
import com.beril.kaomoji.ui.GhostBtn
import com.beril.kaomoji.ui.J
import com.beril.kaomoji.ui.SectionLabel
import com.beril.kaomoji.ui.Small
import com.beril.kaomoji.ui.TitleM
import com.beril.kaomoji.ui.nav.dpadFocusable
import java.io.File

/**
 * Bir kavramın gerçek ilişki grafiği (§16) — hem gezilebilir bir liste (her satır tıklanabilir,
 * grafın kendisi davranışı yönlendiriyor) HEM de kullanıcının açıkça istediği görsel bir
 * node-link çizimi (bkz. `ConnectionGraphCanvas`). İkisi birbirini dışlamıyor: liste erişilebilir
 * etkileşimin (dpad/dokunma) asıl yolu, görsel graf "büyük resmi" bir bakışta görmek için.
 */
@Composable
fun ConceptGraphScreen(
    conceptId: String,
    conceptTitle: String,
    onBack: () -> Unit,
    onOpenConcept: (id: String, title: String) -> Unit,
    onStartSession: (id: String, title: String) -> Unit,
) {
    val ctx = LocalContext.current
    val repo = remember { LabRepository(ctx) }
    val gate = remember { AICapabilityGate.forContext(ctx) }
    val scope = rememberCoroutineScope()

    var prerequisites by remember { mutableStateOf<List<Pair<KnowledgeObjectEntity, String?>>>(emptyList()) }
    var enables by remember { mutableStateOf<List<Pair<KnowledgeObjectEntity, String?>>>(emptyList()) }
    var related by remember { mutableStateOf<List<Pair<KnowledgeObjectEntity, String?>>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var conceptBody by remember { mutableStateOf<String?>(null) }
    var explanation by remember { mutableStateOf<AIResult<String>?>(null) }
    var explaining by remember { mutableStateOf(false) }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasAudioPermission = granted
    }
    val recorder = remember { LabRecorder(ctx) }
    val player = remember { LabPlayer() }
    DisposableEffect(Unit) { onDispose { recorder.cancel(); player.stop() } }
    var isRecording by remember { mutableStateOf(false) }
    var explanations by remember { mutableStateOf<List<KnowledgeObjectEntity>>(emptyList()) }
    var playingId by remember { mutableStateOf<String?>(null) }
    var busyExplanationId by remember { mutableStateOf<String?>(null) }
    var explanationsTick by remember { mutableStateOf(0) }

    // Video anlatım (§ videolu değerlendirme): bu uygulama kendi kamera UI'ını yazmıyor,
    // kaydı sistemin kamera uygulamasına devrediyor — sadece hedef dosyayı/Uri'yi hazırlıyor.
    var pendingVideoFile by remember { mutableStateOf<File?>(null) }
    val videoCaptureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        val file = pendingVideoFile
        pendingVideoFile = null
        if (success && file != null && file.exists() && file.length() > 0) {
            scope.launch {
                repo.createVideoExplanation(conceptId, conceptTitle, file.absolutePath)
                explanationsTick++
            }
        } else {
            file?.delete()
        }
    }

    LaunchedEffect(conceptId, explanationsTick) {
        explanations = repo.explanationsFor(conceptId)
    }

    LaunchedEffect(conceptId) {
        conceptBody = repo.getById(conceptId)?.body
        val buckets = bucketRelationships(conceptId, repo.relationshipsOf(conceptId))
        val prereqs = mutableListOf<Pair<KnowledgeObjectEntity, String?>>()
        val enableList = mutableListOf<Pair<KnowledgeObjectEntity, String?>>()
        val relatedList = mutableListOf<Pair<KnowledgeObjectEntity, String?>>()

        buckets.forEach { edge ->
            val other = repo.getById(edge.otherId) ?: return@forEach
            when (edge.bucket) {
                EdgeBucket.PREREQUISITE -> prereqs += other to edge.note
                EdgeBucket.ENABLES -> enableList += other to edge.note
                EdgeBucket.RELATED -> relatedList += other to edge.note
            }
        }
        prerequisites = prereqs
        enables = enableList
        related = relatedList
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
            Text("🕸️ $conceptTitle", style = Display)
            Spacer(Modifier.height(8.dp))
            Btn("▶ Öğrenme Oturumu Başlat", { onStartSession(conceptId, conceptTitle) })
        }

        if (loaded && (prerequisites.isNotEmpty() || enables.isNotEmpty() || related.isNotEmpty())) {
            item {
                SectionLabel("bağlantı grafiği (görsel)", "🕸️")
                Spacer(Modifier.height(6.dp))
                ConnectionGraphCanvas(conceptTitle, prerequisites, enables, related, onOpenConcept)
            }
        }

        item {
            SectionLabel("ai ile açıkla", "🤖")
            Text(
                "Kendi tahminin/denemen olmadan tam açıklama istemek önerilmez (§13) — ama " +
                    "takıldıysan bir yön bulmak için kullanabilirsin.",
                style = Small,
            )
            Spacer(Modifier.height(6.dp))
            Btn(if (explaining) "İsteniyor…" else "Bu Kavramı Açıkla", {
                if (!explaining) {
                    explaining = true
                    scope.launch {
                        explanation = gate.explainConcept(conceptTitle, conceptBody)
                        explaining = false
                    }
                }
            }, enabled = !explaining, emoji = "🤖")
            Spacer(Modifier.height(6.dp))
            AiResultView(explanation) { text -> Text(text, style = Small) }
        }

        item {
            SectionLabel("anlat (feynman tekniği)", "🎙️")
            Text(
                "Bu kavramı kendi sesinle, defter/kitaba bakmadan anlat — nerede tıkandığın " +
                    "tam olarak bilmediğin yeri gösterir.",
                style = Small,
            )
            Spacer(Modifier.height(6.dp))
            when {
                !hasAudioPermission -> Btn("Mikrofon İzni Ver", {
                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }, emoji = "🎙️")
                !isRecording -> Btn("Anlatmaya Başla", {
                    recorder.start()
                    isRecording = true
                }, emoji = "🎙️")
                else -> Btn("Durdur ve Kaydet", {
                    val file = recorder.stop()
                    isRecording = false
                    if (file != null) {
                        scope.launch {
                            repo.createExplanation(conceptId, conceptTitle, file.absolutePath)
                            explanationsTick++
                        }
                    }
                }, bg = J.cherry, emoji = "⏹")
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Ya da kendini video ile kaydet, sonra izleyerek değerlendir — anlattığını " +
                    "gören bir öğrenci gibi dinle (§ videolu değerlendirme).",
                style = Small,
            )
            Spacer(Modifier.height(6.dp))
            GhostBtn("Video ile Anlat", {
                val file = newVideoFile(ctx)
                pendingVideoFile = file
                videoCaptureLauncher.launch(videoFileUri(ctx, file))
            }, emoji = "📹")
        }

        item { SectionLabel("anlatım geçmişi", "🗂️") }
        if (explanations.isEmpty()) {
            item { Empty("🎙️", "Henüz bir anlatım yok", "Yukarıdan ilk anlatımını kaydet.") }
        } else {
            items(explanations, key = { it.id }) { exp ->
                val payload = runCatching { ExplanationPayload.fromJson(exp.payload) }.getOrNull()
                val isBusy = busyExplanationId == exp.id
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(J.card, RoundedCornerShape(14.dp))
                        .padding(13.dp),
                ) {
                    val isVideo = payload?.videoFilePath != null
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(exp.title, style = TitleM)
                            Text(if (isVideo) "Video Kayıt" else "Ses Kaydı", style = Small.copy(color = J.inkFaint))
                        }
                        if (isVideo) {
                            GhostBtn("▶ İzle (Video)", {
                                val uri = videoFileUri(ctx, File(payload!!.videoFilePath!!))
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "video/mp4")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                runCatching { ctx.startActivity(intent) }
                            })
                        } else {
                            GhostBtn(
                                if (playingId == exp.id) "Durdur" else "▶ Dinle",
                                {
                                    val path = payload?.audioFilePath
                                    if (playingId == exp.id) {
                                        player.stop()
                                        playingId = null
                                    } else if (path != null) {
                                        player.play(path) { playingId = null }
                                        playingId = exp.id
                                    }
                                },
                            )
                        }
                    }
                    if (exp.body != null) {
                        Spacer(Modifier.height(8.dp))
                        Text("Transkript", style = Small.copy(color = J.inkFaint))
                        Text(exp.body, style = Small)
                    }
                    if (payload?.aiEvaluation != null) {
                        Spacer(Modifier.height(8.dp))
                        Text("AI değerlendirmesi", style = Small.copy(color = J.inkFaint))
                        Text(payload.aiEvaluation, style = Small)
                    }
                    Spacer(Modifier.height(8.dp))
                    if (!isVideo) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (exp.body == null) {
                                GhostBtn(if (isBusy) "…" else "🤖 Transkribe Et", {
                                    val path = payload?.audioFilePath
                                    if (!isBusy && path != null) {
                                        busyExplanationId = exp.id
                                        scope.launch {
                                            val result = gate.transcribeAudio(java.io.File(path))
                                            if (result is AIResult.Success) {
                                                repo.attachTranscript(exp.id, result.value)
                                                explanationsTick++
                                            }
                                            busyExplanationId = null
                                        }
                                    }
                                })
                            } else if (payload?.aiEvaluation == null) {
                                val transcript = exp.body
                                GhostBtn(if (isBusy) "…" else "🤖 Analiz Et", {
                                    if (!isBusy) {
                                        busyExplanationId = exp.id
                                        scope.launch {
                                            val result = gate.analyzeTranscript(transcript, conceptTitle)
                                            if (result is AIResult.Success) {
                                                repo.attachEvaluation(exp.id, result.value)
                                                explanationsTick++
                                            }
                                            busyExplanationId = null
                                        }
                                    }
                                })
                            }
                        }
                    }
                }
            }
        }

        item { SectionLabel("önce bunları bilmen gerekiyor", "←") }
        if (!loaded) {
            item { Text("Yükleniyor…", style = Small) }
        } else if (prerequisites.isEmpty()) {
            item { Empty("←", "Önkoşulu yok", "Bu kavram başka hiçbir şeye bağımlı değil — doğrudan başlanabilir.") }
        } else {
            items(prerequisites, key = { "pre-${it.first.id}" }) { (obj, note) ->
                RelatedRow(obj, noteLabel(note), onOpenConcept)
            }
        }

        item { SectionLabel("bu, şunları açıyor", "→") }
        if (loaded && enables.isEmpty()) {
            item { Empty("→", "Henüz hiçbir şeyi açmıyor", "Bu, grafikte başka bir kavramın önkoşulu olarak işaretli değil.") }
        } else {
            items(enables, key = { "en-${it.first.id}" }) { (obj, note) ->
                RelatedRow(obj, noteLabel(note), onOpenConcept)
            }
        }

        item { SectionLabel("ilişkili kavramlar", "🔗") }
        if (loaded && related.isEmpty()) {
            item { Empty("🔗", "Henüz işaretli bir disiplinlerarası bağlantı yok", null) }
        } else {
            items(related, key = { "rel-${it.first.id}" }) { (obj, note) ->
                RelatedRow(obj, note, onOpenConcept)
            }
        }
    }
}

@Composable
private fun RelatedRow(obj: KnowledgeObjectEntity, label: String?, onOpen: (String, String) -> Unit) {
    val hoverSource = remember { MutableInteractionSource() }
    val hovered by hoverSource.collectIsHoveredAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .hoverable(hoverSource)
            .background(if (hovered) J.lime.copy(alpha = 0.3f) else J.card, RoundedCornerShape(14.dp))
            .dpadFocusable(onClick = { onOpen(obj.id, obj.title) }, shape = RoundedCornerShape(14.dp))
            .padding(13.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(obj.title, style = TitleM)
            Text(if (hovered) "Dokun → aç" else (label ?: ""), style = Small.copy(color = J.inkFaint))
        }
    }
}

private fun noteLabel(note: String?): String? = when (note) {
    "hard" -> "ZORUNLU ÖNKOŞUL"
    "soft" -> "YUMUŞAK ÖNKOŞUL — hafif bir işleniş bununsuz da mümkün"
    "tool" -> "ARAÇ OLARAK KULLANILIYOR — kendi kuramı gerekmiyor"
    "intuition" -> "SEZGİ / BENZETME — öğrenmeyi hızlandırır"
    "co-requisite" -> "BİRLİKTE ÇALIŞILMALI"
    else -> note
}

/** Önkoşul kenarının türüne göre renk (bkz. [noteLabel]) — graf artık sadece "bağlı" demiyor,
 *  NE TÜR bağlı olduğunu renkle de söylüyor (zorunlu/yumuşak/araç/sezgi/birlikte-çalışılmalı). */
private fun edgeColor(note: String?): Color = when (note) {
    "hard" -> J.cherry
    "soft" -> J.blush
    "tool" -> J.butter
    "intuition" -> J.bark
    "co-requisite" -> J.forest
    else -> J.inkFaint
}

/**
 * Kullanıcının isteği üzerine eklenen gerçek bir node-link çizimi (§16'nın orijinal "süs bir
 * diyagram değil" kararını bilerek tersine çeviriyor — Aşama 25'in `generateCurriculum` kararını
 * tersine çevirmesiyle aynı mantık: kullanıcı açıkça istedi). Konum hesaplaması basit ve sabit —
 * önkoşullar solda, merkez ortada, "bunu açıyor" sağda, ilişkili kavramlar altta bir sırada.
 * Gerçek bir graf-yerleşim algoritması (force-directed vb.) değil; küçük sayıda düğüm için
 * (bu ekranın gerçek kullanımı) yeterli ve öngörülebilir — ama artık (Aşama 28) hiçbir düğüm
 * gizlenmiyor: tüm ilişkili kavramlar gösteriliyor, genişlik ekranı aşarsa yatay kaydırılıyor
 * (önceden en fazla 6 ilişkili kavramla sınırlıydı). Kenarlar artık önkoşul TÜRÜNE göre
 * renkli (zorunlu/yumuşak/araç/sezgi) — alttaki bir renk lejandıyla açıklanıyor.
 */
@Composable
private fun ConnectionGraphCanvas(
    centerTitle: String,
    prerequisites: List<Pair<KnowledgeObjectEntity, String?>>,
    enables: List<Pair<KnowledgeObjectEntity, String?>>,
    related: List<Pair<KnowledgeObjectEntity, String?>>,
    onOpenConcept: (String, String) -> Unit,
) {
    val nodeW = 104.dp
    val nodeH = 46.dp
    val rowGap = 10.dp
    val colGap = 56.dp

    val sideCount = maxOf(prerequisites.size, enables.size, 1)
    val sideBlockHeight = nodeH * sideCount + rowGap * (sideCount - 1).coerceAtLeast(0)
    val hasRelated = related.isNotEmpty()
    val topPad = 4.dp
    val centerY = topPad + sideBlockHeight / 2
    val relatedY = topPad + sideBlockHeight + rowGap * 2
    val totalHeight = relatedY + (if (hasRelated) nodeH else 0.dp) + 8.dp
    val centerX = nodeW + colGap + nodeW / 2
    val relatedRowWidth = nodeW * related.size + rowGap * (related.size - 1).coerceAtLeast(0)
    val sideWidth = nodeW * 3 + colGap * 2
    val totalWidth = if (relatedRowWidth > sideWidth) relatedRowWidth else sideWidth

    Row(Modifier.horizontalScroll(rememberScrollState())) {
        Box(Modifier.width(totalWidth).height(totalHeight)) {
            Canvas(Modifier.matchParentSize()) {
                val cx = centerX.toPx()
                val cy = centerY.toPx()
                val halfNodeW = nodeW.toPx() / 2
                prerequisites.forEachIndexed { i, (_, note) ->
                    val y = (topPad + nodeH / 2 + (nodeH + rowGap) * i).toPx()
                    drawLine(edgeColor(note), Offset(nodeW.toPx(), y), Offset(cx - halfNodeW, cy), strokeWidth = 2.5f)
                }
                enables.forEachIndexed { i, (_, note) ->
                    val y = (topPad + nodeH / 2 + (nodeH + rowGap) * i).toPx()
                    drawLine(edgeColor(note), Offset(cx + halfNodeW, cy), Offset(nodeW.toPx() * 2 + colGap.toPx(), y), strokeWidth = 2.5f)
                }
                if (hasRelated) {
                    val y = (relatedY + nodeH / 2).toPx()
                    related.forEachIndexed { i, _ ->
                        val x = ((nodeW + rowGap) * i + nodeW / 2).toPx()
                        drawLine(J.lime, Offset(cx, cy + nodeH.toPx() / 2), Offset(x, y), strokeWidth = 2f)
                    }
                }
            }

            prerequisites.forEachIndexed { i, (obj, note) ->
                GraphNodeChip(
                    obj.title,
                    Modifier.offset(x = 0.dp, y = topPad + (nodeH + rowGap) * i),
                    onClick = { onOpenConcept(obj.id, obj.title) },
                    width = nodeW, height = nodeH, accentColor = edgeColor(note),
                )
            }
            GraphNodeChip(
                centerTitle,
                Modifier.offset(x = nodeW + colGap, y = centerY - nodeH / 2),
                onClick = {}, width = nodeW, height = nodeH, highlighted = true,
            )
            enables.forEachIndexed { i, (obj, note) ->
                GraphNodeChip(
                    obj.title,
                    Modifier.offset(x = nodeW * 2 + colGap * 2, y = topPad + (nodeH + rowGap) * i),
                    onClick = { onOpenConcept(obj.id, obj.title) },
                    width = nodeW, height = nodeH, accentColor = edgeColor(note),
                )
            }
            related.forEachIndexed { i, (obj, _) ->
                GraphNodeChip(
                    obj.title,
                    Modifier.offset(x = (nodeW + rowGap) * i, y = relatedY),
                    onClick = { onOpenConcept(obj.id, obj.title) },
                    width = nodeW, height = nodeH, accentColor = J.lime,
                )
            }
        }
    }
    if (prerequisites.any { it.second != null } || enables.any { it.second != null }) {
        Spacer(Modifier.height(6.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("hard", "soft", "tool", "intuition", "co-requisite").forEach { key ->
                LegendDot(edgeColor(key), noteLabel(key)?.substringBefore(" —") ?: key)
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(8.dp).height(8.dp).background(color, RoundedCornerShape(4.dp)))
        Spacer(Modifier.width(4.dp))
        Text(label, style = Small.copy(color = J.inkFaint))
    }
}

@Composable
private fun GraphNodeChip(
    title: String,
    modifier: Modifier,
    onClick: () -> Unit,
    width: Dp,
    height: Dp,
    highlighted: Boolean = false,
    accentColor: Color? = null,
) {
    Box(
        modifier
            .width(width)
            .height(height)
            .background(if (highlighted) J.forest else J.card, RoundedCornerShape(10.dp))
            .then(
                if (accentColor != null && !highlighted) {
                    Modifier.border(1.5.dp, accentColor, RoundedCornerShape(10.dp))
                } else Modifier,
            )
            .then(if (highlighted) Modifier else Modifier.dpadFocusable(onClick = onClick, shape = RoundedCornerShape(10.dp)))
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            title,
            style = Small.copy(color = if (highlighted) J.card else J.ink, textAlign = TextAlign.Center),
            maxLines = 2,
        )
    }
}
