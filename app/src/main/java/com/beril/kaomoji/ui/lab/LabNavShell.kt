package com.beril.kaomoji.ui.lab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.beril.kaomoji.ui.nav.dpadFocusable

/**
 * Portrait-tablet öncelikli üç katmanlı kabuk (§7/§8/§31). Birincil hedef kullanım — tablet
 * dik tutulduğunda — TABLET_PORTRAIT: kalıcı sol rail + içerik. TABLET_LANDSCAPE'te rail'in
 * yanına bir `secondaryPanel` eklenebilir (örn. bir öğrenme oturumu açıldığında ana liste
 * arkada kalmaz, yan yana görünür — §38 "nereden geldiğini görsel olarak koru"). COMPACT'ta
 * (katlanabilir kapalı / telefon genişliği) rail hiç gösterilmez, `content` tam ekran kaplar.
 *
 * Rail artık §31'in istediği beş kalıcı üst-seviye alanı taşıyor — Öğren/Bilgi/Projeler/
 * Akademik/Arşiv — tek bir "Ana Sayfa" öğesi değil: Lab artık bu beş alanın her birinde
 * gerçek, ayrı bir giriş noktasına sahip (bkz. `LabRoot`'un alan+ekran durumu). AI Ayarları,
 * bu beş alandan biri OLMADIĞI için rail'in altına sabit, ayrı bir dişli simgesi olarak
 * eklendi — beş alanın anlamını zorlayarak bir altıncı sahte alan icat etmek yerine.
 */
@Composable
fun LabNavShell(
    currentArea: LabArea,
    onSelectArea: (LabArea) -> Unit,
    onOpenSettings: () -> Unit,
    secondaryPanel: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        when (breakpointFor(maxWidth.value.toInt())) {
            LabBreakpoint.COMPACT -> content()

            LabBreakpoint.TABLET_PORTRAIT -> Row(Modifier.fillMaxSize()) {
                LabRail(currentArea, onSelectArea, onOpenSettings)
                Box(Modifier.weight(1f).fillMaxHeight()) { content() }
            }

            LabBreakpoint.TABLET_LANDSCAPE -> Row(Modifier.fillMaxSize()) {
                LabRail(currentArea, onSelectArea, onOpenSettings)
                Box(Modifier.weight(1f).fillMaxHeight()) { content() }
                if (secondaryPanel != null) {
                    Box(Modifier.weight(1f).fillMaxHeight().background(J.paper)) { secondaryPanel() }
                }
            }
        }
    }
}

@Composable
private fun LabRail(currentArea: LabArea, onSelectArea: (LabArea) -> Unit, onOpenSettings: () -> Unit) {
    Column(
        Modifier.width(76.dp).fillMaxHeight().background(J.paperDeep),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        LabArea.entries.forEach { area ->
            LabRailItem(area.emoji, area.label, selected = area == currentArea, onClick = { onSelectArea(area) })
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.weight(1f))
        LabRailItem("⚙️", "Ayarlar", selected = false, onClick = onOpenSettings)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun LabRailItem(emoji: String, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .background(if (selected) J.card else J.card.copy(alpha = 0f), RoundedCornerShape(12.dp))
            .dpadFocusable(onClick = onClick, shape = RoundedCornerShape(12.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, style = TextStyle(fontSize = 20.sp))
        Spacer(Modifier.height(2.dp))
        Text(label, style = Small.copy(textAlign = TextAlign.Center))
    }
}
