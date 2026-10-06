package com.beril.kaomoji.ui.lab2

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.ContextCompat
import com.beril.kaomoji.ai.engine.AICapabilityGate
import com.beril.kaomoji.ai.engine.AIResult
import com.beril.kaomoji.lab.audio.LabPlayer
import com.beril.kaomoji.lab.audio.LabRecorder
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

/**
 * Bir kavramın gerçek ilişki grafiği (§16) — süs bir diyagram değil, gezilebilir bir liste:
 * hangi kavramlar bunu önkoşul olarak istiyor, bu hangilerini açıyor, hangi disiplinlerarası
 * bağlantılar var. Her satır tıklanabilir — graf burada gerçekten davranışı yönlendiriyor
 * (bir kavramdan diğerine geçmeni sağlıyor), dekoratif bir node-link çizimi değil.
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
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(exp.title, style = TitleM)
                            Text("Kayıt", style = Small.copy(color = J.inkFaint))
                        }
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
    Row(
        Modifier
            .fillMaxWidth()
            .background(J.card, RoundedCornerShape(14.dp))
            .dpadFocusable(onClick = { onOpen(obj.id, obj.title) }, shape = RoundedCornerShape(14.dp))
            .padding(13.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(obj.title, style = TitleM)
            if (label != null) Text(label, style = Small.copy(color = J.inkFaint))
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
