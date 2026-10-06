package com.beril.kaomoji.ui.lab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.beril.kaomoji.lab.learning.LearningDiscipline
import com.beril.kaomoji.lab.learning.LearningSessionState
import com.beril.kaomoji.lab.learning.LearningStage
import com.beril.kaomoji.lab.repository.LabRepository
import com.beril.kaomoji.ui.Btn
import com.beril.kaomoji.ui.Display
import com.beril.kaomoji.ui.Field
import com.beril.kaomoji.ui.GhostBtn
import com.beril.kaomoji.ui.J
import com.beril.kaomoji.ui.Selector
import com.beril.kaomoji.ui.Small
import com.beril.kaomoji.ui.TitleL
import kotlinx.coroutines.launch

/**
 * Soru-önce öğrenme oturumu (§12-14). Disiplin seçilince aşama dizisi [LearningSessionEngine]
 * tarafından belirlenir — bu ekran hiçbir aşamayı kendisi icat etmiyor, sadece durumu gösterip
 * "ilerle" dediğinde [LabRepository.saveLearningSessionState] ile kalıcı hale getiriyor.
 * Kullanıcının denemesi (attemptText) her aşamada korunur, asla sıfırlanmaz.
 */
@Composable
fun LearningSessionScreen(conceptId: String, conceptTitle: String, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { LabRepository(ctx) }
    val scope = rememberCoroutineScope()

    var sessionId by remember { mutableStateOf<String?>(null) }
    var state by remember { mutableStateOf<LearningSessionState?>(null) }
    var discipline by remember { mutableStateOf(LearningDiscipline.GENERAL) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, 12.dp, 14.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            GhostBtn("Geri", onBack, emoji = "←")
            Spacer(Modifier.height(10.dp))
            Text("🧠 $conceptTitle", style = Display)
        }

        if (sessionId == null) {
            item {
                Text("Bu kavram hangi disiplinin ritmine daha yakın? Aşama dizisi buna göre değişir.", style = Small)
                Spacer(Modifier.height(8.dp))
                Selector(
                    options = LearningDiscipline.entries.map { it.name },
                    selected = discipline.name,
                    onSelect = { discipline = LearningDiscipline.valueOf(it) },
                    labels = { disciplineLabel(LearningDiscipline.valueOf(it)) },
                )
                Spacer(Modifier.height(10.dp))
                Btn("Oturumu Başlat", {
                    scope.launch {
                        val id = repo.startLearningSession(conceptId, discipline)
                        sessionId = id
                        state = repo.getLearningSessionState(id)
                    }
                }, emoji = "▶")
            }
        } else {
            val s = state
            if (s == null) {
                item { Text("Yükleniyor…", style = Small) }
            } else if (s.completed) {
                item {
                    Column(
                        Modifier.fillMaxWidth().background(J.lime.copy(alpha = 0.25f), RoundedCornerShape(16.dp)).padding(16.dp),
                    ) {
                        Text("✓ Oturum tamamlandı", style = TitleL)
                        Text("Tüm aşamalardan geçtin. Denemen kalıcı olarak kaydedildi.", style = Small)
                    }
                }
                item { GhostBtn("Kavramlara dön", onBack, emoji = "←") }
            } else {
                item { StageHeader(s) }
                item { StageBody(s, onAttemptChange = { text -> state = s.withAttempt(text) }) }
                item {
                    Btn(if (s.isLastStage) "Tamamla" else "İlerle →", {
                        val next = s.advance()
                        state = next
                        scope.launch { sessionId?.let { repo.saveLearningSessionState(it, next) } }
                    }, emoji = if (s.isLastStage) "✓" else "→")
                }
            }
        }
    }
}

@Composable
private fun StageHeader(s: LearningSessionState) {
    Column {
        Text("Aşama ${s.stageIndex + 1} / ${s.stages.size}", style = Small.copy(color = J.inkFaint))
        Text(stageLabel(s.currentStage), style = TitleL)
    }
}

@Composable
private fun StageBody(s: LearningSessionState, onAttemptChange: (String) -> Unit) {
    when (s.currentStage) {
        LearningStage.ENCOUNTER -> Text("Konuyla ilk karşılaşma — ne bildiğini/tahmin ettiğini not et.", style = Small)
        LearningStage.QUESTION, LearningStage.ATTEMPT -> Column {
            Text("Önce kendi tahminini/denemeni yaz — henüz bir açıklama göstermeden (§13).", style = Small)
            Spacer(Modifier.height(6.dp))
            Field(s.attemptText, onAttemptChange, placeholder = "Tahminim / denemem...", minLines = 3)
        }
        LearningStage.STRUGGLE -> Text("Takıldıysan sorun değil — bu veri. İlerlemeden önce bir kez daha dene.", style = Small)
        LearningStage.REVEAL -> Text("Şimdi sadece ihtiyacın olan kısmı aç — hepsini birden değil.", style = Small)
        LearningStage.PRACTICE, LearningStage.VARIATION -> Text("Aynı fikri farklı bir örnekte uygula.", style = Small)
        LearningStage.APPLICATION -> Text("Bunu gerçek bir probleme/projeye bağla.", style = Small)
        LearningStage.DERIVATION -> Text("Sonucu sıfırdan türetmeyi/ispatlamayı dene.", style = Small)
        LearningStage.EXPLAIN_OWN_WORDS -> Column {
            Text("Bunu kendi sözlerinle, birine öğretir gibi anlat.", style = Small)
            Spacer(Modifier.height(6.dp))
            Field(s.attemptText, onAttemptChange, placeholder = "Kendi açıklamam...", minLines = 3)
        }
        LearningStage.REFLECT -> Text("Bu oturumda ne değişti? Hangi kısım beklenenden kolay/zordu?", style = Small)
        LearningStage.RETAIN -> Text("Bunu aralıklı tekrara eklemeyi düşün.", style = Small)
        LearningStage.CONNECT -> Text("Bu başka hangi kavram/projeyle ilişkili?", style = Small)
    }
}

private fun disciplineLabel(d: LearningDiscipline): String = when (d) {
    LearningDiscipline.MATH -> "Matematik"
    LearningDiscipline.PHYSICS -> "Fizik"
    LearningDiscipline.PROGRAMMING -> "Programlama"
    LearningDiscipline.THEORY -> "Teori"
    LearningDiscipline.GENERAL -> "Genel"
}

private fun stageLabel(stage: LearningStage): String = when (stage) {
    LearningStage.ENCOUNTER -> "Karşılaşma"
    LearningStage.QUESTION -> "Soru"
    LearningStage.ATTEMPT -> "Deneme"
    LearningStage.STRUGGLE -> "Takılma"
    LearningStage.REVEAL -> "Açığa Çıkarma"
    LearningStage.PRACTICE -> "Pratik"
    LearningStage.VARIATION -> "Çeşitleme"
    LearningStage.APPLICATION -> "Uygulama"
    LearningStage.DERIVATION -> "Türetim / İspat"
    LearningStage.EXPLAIN_OWN_WORDS -> "Kendi Sözlerinle Anlat"
    LearningStage.REFLECT -> "Değerlendirme"
    LearningStage.RETAIN -> "Kalıcılık"
    LearningStage.CONNECT -> "Bağlantı"
}
