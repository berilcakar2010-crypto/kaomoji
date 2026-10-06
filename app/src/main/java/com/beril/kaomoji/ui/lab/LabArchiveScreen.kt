package com.beril.kaomoji.ui.lab

import android.content.Intent
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.beril.kaomoji.ai.engine.AICapabilityGate
import com.beril.kaomoji.ai.engine.AIResult
import com.beril.kaomoji.lab.audio.LabPlayer
import com.beril.kaomoji.lab.audio.videoFileUri
import com.beril.kaomoji.lab.model.ExplanationPayload
import com.beril.kaomoji.lab.repository.ExplanationWithConcept
import com.beril.kaomoji.lab.repository.LabRepository
import java.io.File
import com.beril.kaomoji.ui.Display
import com.beril.kaomoji.ui.Empty
import com.beril.kaomoji.ui.GhostBtn
import com.beril.kaomoji.ui.J
import com.beril.kaomoji.ui.SectionLabel
import com.beril.kaomoji.ui.Small
import com.beril.kaomoji.ui.TitleM
import kotlinx.coroutines.launch

/**
 * "Arşiv" alanının ana ekranı (§31) — eski uygulamanın `AudioLibraryScreen`'i gibi, kavramdan
 * bağımsız, uygulamadaki TÜM anlatım kayıtlarının (ses VE video) tek bir listesi.
 * `ConceptGraphScreen`'in anlatım bölümü (Aşama 23/video Aşama 27) bir kavramla sınırlı; bu
 * ekran o sınırı kaldırıyor — "tüm kayıtlarım" sorusuna artık her zaman bir cevap var. Ses
 * kayıtları kendi transkripsiyon/analiz adımlarını taşır (Aşama 24); video kayıtları sadece
 * izlenir — sistemin video oynatıcısına devredilir, AI analiz edilmez (bkz. `ConceptGraphScreen`
 * dosyasındaki not: ses transkripsiyon isteği mime_type'ı sabit "audio/mp4" işaretliyor, bir
 * video dosyasını aynı yoldan göndermek sessizce yanlış sonuç üretebilir). Her satırın kaynak
 * kavrama geri dönüş linki var. Verim (dışa/içe aktarma, yedekleme) de buraya bağlı.
 */
@Composable
fun LabArchiveScreen(onOpenConcept: (id: String, title: String) -> Unit, onOpenData: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { LabRepository(ctx) }
    val gate = remember { AICapabilityGate.forContext(ctx) }
    val scope = rememberCoroutineScope()

    val player = remember { LabPlayer() }
    DisposableEffect(Unit) { onDispose { player.stop() } }

    var recordings by remember { mutableStateOf<List<ExplanationWithConcept>>(emptyList()) }
    var playingId by remember { mutableStateOf<String?>(null) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var tick by remember { mutableStateOf(0) }

    LaunchedEffect(tick) { recordings = repo.allExplanations() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, 12.dp, 14.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("🗄️ Arşiv", style = Display)
            Text(
                "Hangi kavramla kaydettiğinden bağımsız, yaptığın tüm anlatım kayıtları — " +
                    "ve verilerinin yedeklenmesi/taşınması.",
                style = Small,
            )
            Spacer(Modifier.height(10.dp))
            GhostBtn("Verim (Yedekleme / İçe-Dışa Aktarma)", onOpenData, emoji = "📁")
        }

        item { SectionLabel("tüm anlatım kayıtları", "🗂️") }
        if (recordings.isEmpty()) {
            item {
                Empty(
                    "🎙️",
                    "Henüz bir kayıt yok",
                    "Bir kavramın sayfasından \"Anlat (Feynman tekniği)\" ile (sesli ya da video) ilk kaydını yap, burada görünecek.",
                )
            }
        } else {
            items(recordings, key = { it.explanation.id }) { entry ->
                val exp = entry.explanation
                val payload = runCatching { ExplanationPayload.fromJson(exp.payload) }.getOrNull()
                val isBusy = busyId == exp.id
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
                            Text(
                                entry.conceptTitle ?: "Bağlı kavram bulunamadı",
                                style = Small.copy(color = J.inkFaint),
                            )
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!isVideo) {
                            if (exp.body == null) {
                                GhostBtn(if (isBusy) "…" else "🤖 Transkribe Et", {
                                    val path = payload?.audioFilePath
                                    if (!isBusy && path != null) {
                                        busyId = exp.id
                                        scope.launch {
                                            val result = gate.transcribeAudio(java.io.File(path))
                                            if (result is AIResult.Success) {
                                                repo.attachTranscript(exp.id, result.value)
                                                tick++
                                            }
                                            busyId = null
                                        }
                                    }
                                })
                            } else if (payload?.aiEvaluation == null) {
                                val transcript = exp.body
                                GhostBtn(if (isBusy) "…" else "🤖 Analiz Et", {
                                    if (!isBusy) {
                                        busyId = exp.id
                                        scope.launch {
                                            val result = gate.analyzeTranscript(transcript, entry.conceptTitle ?: exp.title)
                                            if (result is AIResult.Success) {
                                                repo.attachEvaluation(exp.id, result.value)
                                                tick++
                                            }
                                            busyId = null
                                        }
                                    }
                                })
                            }
                        }
                        if (entry.conceptId != null) {
                            GhostBtn("Kavrama Git →", { onOpenConcept(entry.conceptId, entry.conceptTitle ?: "") })
                        }
                    }
                }
            }
        }
    }
}
