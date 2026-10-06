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
 * "Bilgi" alanının ana ekranı (§31) — bilgi grafiğini aramak ve kendi yazdığın bir metni
 * (deneme taslağı, not, her ne olursa) AI'a düzenlettirmek/organize ettirmek için giriş noktası.
 */
@Composable
fun LabKnowledgeHomeScreen(onOpenSearch: () -> Unit, onOpenWriting: () -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, 12.dp, 14.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("📚 Bilgi", style = Display)
            Text(
                "Bilgi grafiğinde ara, ya da yazdığın bir metni AI'a göster — netlik için " +
                    "düzenlesin ya da notlarını organize etsin.",
                style = Small,
            )
            Spacer(Modifier.height(10.dp))
            GhostBtn("Bilgi Grafiğinde Ara", onOpenSearch, emoji = "🔎")
            Spacer(Modifier.height(8.dp))
            GhostBtn("Yazım Yardımı", onOpenWriting, emoji = "✍️")
        }
    }
}
