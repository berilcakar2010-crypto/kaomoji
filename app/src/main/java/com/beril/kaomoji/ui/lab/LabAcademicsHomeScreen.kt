package com.beril.kaomoji.ui.lab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.beril.kaomoji.ui.Display
import com.beril.kaomoji.ui.GhostBtn
import com.beril.kaomoji.ui.Small

/**
 * "Akademik" alanının ana ekranı (§31) — kendi öğrenmenin üzerine çıkıp bakma: genel
 * değerlendirme, geçmiş hatalardan öğrenme, tekrar kartları ile hatırlama.
 */
@Composable
fun LabAcademicsHomeScreen(
    onOpenEvaluation: () -> Unit,
    onOpenMistakes: () -> Unit,
    onOpenFlashcards: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, 12.dp, 14.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("🪞 Akademik", style = Display)
            Text("Değerlendirme, hata defteri, tekrar kartları.", style = Small)
            Spacer(Modifier.height(10.dp))
            GhostBtn("Değerlendir", onOpenEvaluation, emoji = "🪞")
            Spacer(Modifier.height(8.dp))
            GhostBtn("Hata Defteri", onOpenMistakes, emoji = "⚠️")
            Spacer(Modifier.height(8.dp))
            GhostBtn("Tekrar Kartları", onOpenFlashcards, emoji = "🃏")
        }
    }
}
