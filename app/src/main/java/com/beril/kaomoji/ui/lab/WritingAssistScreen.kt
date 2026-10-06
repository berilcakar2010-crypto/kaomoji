package com.beril.kaomoji.ui.lab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.beril.kaomoji.ui.Btn
import com.beril.kaomoji.ui.Display
import com.beril.kaomoji.ui.Field
import com.beril.kaomoji.ui.GhostBtn
import com.beril.kaomoji.ui.SectionLabel
import com.beril.kaomoji.ui.Small
import kotlinx.coroutines.launch

/**
 * Genel amaçlı yazım/not yardımı (§5 Writing, §5 Research) — buraya kadar `improveWriting`
 * sadece `ProjectsScreen`'in notlar alanına, `organizeResearchNotes` ise HİÇBİR ekrana
 * gömülüydü (ikisi de `AICapabilityGate`'te vardı, çağıran yoktu). Herhangi bir metni —
 * Brain Inbox'tan kopyalanmış bir not, bir deneme taslağı, ders çalışması — buraya
 * yapıştırıp iki yoldan birini deneyebilirsin. Sonuç her zaman AYRI gösterilir; "Bu metni
 * kullan" demeden hiçbir şey sessizce değişmez (§5).
 */
@Composable
fun WritingAssistScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val gate = remember { AICapabilityGate.forContext(ctx) }
    val scope = rememberCoroutineScope()

    var text by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<AIResult<String>?>(null) }
    var working by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, 12.dp, 14.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            GhostBtn("Geri", onBack, emoji = "←")
            Spacer(Modifier.height(10.dp))
            Text("✍️ Yazım & Not Yardımı", style = Display)
            Text(
                "Herhangi bir metni (deneme taslağı, ders notu, araştırma notu) buraya " +
                    "yapıştır ya da yaz. Sonuç ayrı gösterilir — \"Bu metni kullan\" demeden " +
                    "hiçbir şey değişmez.",
                style = Small,
            )
        }

        item {
            SectionLabel("metin", "📝")
            Spacer(Modifier.height(6.dp))
            Field(text, { text = it }, placeholder = "Metnini buraya yaz ya da yapıştır…", minLines = 6)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn(if (working) "İsteniyor…" else "Yazımı İyileştir", {
                    if (!working && text.isNotBlank()) {
                        working = true
                        scope.launch {
                            result = gate.improveWriting(text, "netlik ve dilbilgisini düzelt, anlamı değiştirme")
                            working = false
                        }
                    }
                }, enabled = !working, emoji = "✏️")
                GhostBtn(if (working) "İsteniyor…" else "Notlarımı Düzenle", {
                    if (!working && text.isNotBlank()) {
                        working = true
                        scope.launch {
                            result = gate.organizeResearchNotes(text)
                            working = false
                        }
                    }
                }, emoji = "🗂️")
            }
        }

        item {
            AiResultView(result) { value ->
                Column {
                    SectionLabel("sonuç", "✨")
                    Spacer(Modifier.height(6.dp))
                    Text(value, style = Small)
                    Spacer(Modifier.height(6.dp))
                    GhostBtn("Bu metni kullan", { text = value; result = null }, emoji = "↩")
                }
            }
        }
    }
}
