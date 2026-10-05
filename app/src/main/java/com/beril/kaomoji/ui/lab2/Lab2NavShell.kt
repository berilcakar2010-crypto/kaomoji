package com.beril.kaomoji.ui.lab2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beril.kaomoji.ui.J
import com.beril.kaomoji.ui.Small

/**
 * Portrait-tablet öncelikli üç katmanlı kabuk (§7/§8/§31). Birincil hedef kullanım — tablet
 * dik tutulduğunda — TABLET_PORTRAIT: kalıcı sol rail + içerik. TABLET_LANDSCAPE'te rail'in
 * yanına bir `secondaryPanel` eklenebilir (örn. bir öğrenme oturumu açıldığında ana liste
 * arkada kalmaz, yan yana görünür — §38 "nereden geldiğini görsel olarak koru"). COMPACT'ta
 * (katlanabilir kapalı / telefon genişliği) rail hiç gösterilmez, `content` tam ekran kaplar —
 * bugünkü tek-ekran deneyimle birebir aynı, geriye dönük uyumlu.
 *
 * Bilerek tek rail öğesiyle başlıyor ("Ana Sayfa") — Lab 2.0'ın şu an gerçekten sadece bir üst
 * düzey alanı var; boş sekmelerle sahte bir 6-sekmeli gezinme inşa etmek yerine, yeni bir alan
 * gerçekten var olduğunda rail'e eklenecek.
 */
@Composable
fun Lab2NavShell(
    secondaryPanel: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        when (breakpointFor(maxWidth.value.toInt())) {
            Lab2Breakpoint.COMPACT -> content()

            Lab2Breakpoint.TABLET_PORTRAIT -> Row(Modifier.fillMaxSize()) {
                Lab2Rail()
                Box(Modifier.weight(1f).fillMaxHeight()) { content() }
            }

            Lab2Breakpoint.TABLET_LANDSCAPE -> Row(Modifier.fillMaxSize()) {
                Lab2Rail()
                Box(Modifier.weight(1f).fillMaxHeight()) { content() }
                if (secondaryPanel != null) {
                    Box(Modifier.weight(1f).fillMaxHeight().background(J.paper)) { secondaryPanel() }
                }
            }
        }
    }
}

@Composable
private fun Lab2Rail() {
    Column(
        Modifier.width(76.dp).fillMaxHeight().background(J.paperDeep),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier
                .padding(horizontal = 8.dp, vertical = 10.dp)
                .background(J.card, RoundedCornerShape(12.dp))
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🧪", style = TextStyle(fontSize = 20.sp))
            Spacer(Modifier.height(2.dp))
            Text("Ana\nSayfa", style = Small.copy(textAlign = TextAlign.Center))
        }
    }
}
