package com.beril.kaomoji.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beril.kaomoji.data.Bridge
import com.beril.kaomoji.data.Store
import kotlin.math.cos
import kotlin.math.sin

/**
 * Gelişmiş köprü grafiği: curriculum.json'daki disiplinlerarası "bridges"
 * verisinden çıkarılmış bir ağ. Her köprünün konu etiketleri (t: List<String>)
 * bir düğüm kümesi oluşturuyor; aynı köprüde geçen konular birbirine bağlı.
 *
 * Basit dairesel yerleşim kullanıyoruz (force-directed fizik motoru yok) —
 * az düğüm sayısında (~15-20) okunabilirliği force-directed'den daha iyi.
 */
@Composable
fun BridgeGraphScreen(store: Store, onBack: () -> Unit) {
    val c = store.curriculum
    val bridges = c.bridges

    // ── düğümleri çıkar: tüm köprülerdeki benzersiz konu etiketleri ──
    val nodes = remember(bridges) {
        bridges.flatMap { it.topics }.distinct()
    }
    // ── kenarları çıkar: aynı köprüdeki her konu çifti bağlı ──
    data class Edge(val a: String, val b: String, val bridge: Bridge)
    val edges = remember(bridges) {
        bridges.flatMap { br ->
            val ts = br.topics
            buildList {
                for (i in ts.indices) for (j in i + 1 until ts.size) {
                    add(Edge(ts[i], ts[j], br))
                }
            }
        }
    }

    var selectedNode by remember { mutableStateOf<String?>(null) }
    var selectedBridge by remember { mutableStateOf<Bridge?>(null) }

    Column(Modifier.fillMaxSize().background(J.paper)) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp, 12.dp, 14.dp, 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GhostBtn("Geri", onBack, emoji = "←")
            Spacer(Modifier.width(10.dp))
            Column {
                Text("🕸️ Köprü Grafiği", style = Display)
                Text("${nodes.size} konu · ${bridges.size} köprü — bir düğüme dokun", style = Tiny)
            }
        }

        // ── grafik alanı ──
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(320.dp)
                .padding(10.dp)
                .background(J.card, RoundedCornerShape(18.dp))
                .border(1.dp, J.lineSoft, RoundedCornerShape(18.dp))
        ) {
            val wPx = constraints.maxWidth.toFloat()
            val hPx = constraints.maxHeight.toFloat()
            val cx = wPx / 2f
            val cy = hPx / 2f
            val radius = (minOf(wPx, hPx) / 2f) * 0.72f
            val density = LocalDensity.current

            val positions = remember(nodes, wPx, hPx) {
                nodes.mapIndexed { i, n ->
                    val angle = (2 * Math.PI * i / nodes.size.coerceAtLeast(1)) - Math.PI / 2
                    n to Offset(
                        cx + radius * cos(angle).toFloat(),
                        cy + radius * sin(angle).toFloat()
                    )
                }.toMap()
            }

            // kenarlar
            Canvas(Modifier.fillMaxSize()) {
                edges.forEach { e ->
                    val pa = positions[e.a] ?: return@forEach
                    val pb = positions[e.b] ?: return@forEach
                    val touches = selectedNode != null && (e.a == selectedNode || e.b == selectedNode)
                    val faded = selectedNode != null && !touches
                    drawLine(
                        color = if (touches) J.forest else J.inkFaint.copy(alpha = if (faded) 0.12f else 0.35f),
                        start = pa, end = pb,
                        strokeWidth = if (touches) 3f else 1.5f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // düğümler
            positions.forEach { (name, pos) ->
                val isSelected = name == selectedNode
                val related = edges.any { it.a == name || it.b == name }
                Box(
                    Modifier
                        .offset(
                            x = with(density) { (pos.x - 34).toDp() },
                            y = with(density) { (pos.y - 16).toDp() }
                        )
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (isSelected) J.forest else J.paperDeep,
                            RoundedCornerShape(50)
                        )
                        .border(
                            1.dp,
                            if (isSelected) J.forest else J.lineSoft,
                            RoundedCornerShape(50)
                        )
                        .clickable {
                            selectedNode = if (isSelected) null else name
                            selectedBridge = null
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .widthIn(max = 90.dp)
                ) {
                    Text(
                        name,
                        maxLines = 2,
                        style = TextStyle(
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else J.ink
                        )
                    )
                }
            }
        }

        // ── seçili düğüme bağlı köprüler ──
        val relevantBridges = remember(selectedNode, bridges) {
            if (selectedNode == null) bridges
            else bridges.filter { selectedNode in it.topics }
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(14.dp, 8.dp, 14.dp, 90.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SectionLabel(
                    if (selectedNode == null) "tüm köprüler" else "\"$selectedNode\" ile ilgili köprüler",
                    "🔗"
                )
            }
            items(relevantBridges) { b ->
                val expanded = b == selectedBridge
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(J.card, RoundedCornerShape(14.dp))
                        .border(1.dp, J.lineSoft, RoundedCornerShape(14.dp))
                        .clickable { selectedBridge = if (expanded) null else b }
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(b.emoji, style = TextStyle(fontSize = 16.sp))
                        Spacer(Modifier.width(8.dp))
                        Text(b.name, style = TitleM, modifier = Modifier.weight(1f))
                    }
                    if (expanded) {
                        Spacer(Modifier.height(6.dp))
                        Text(b.desc, style = Small)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            b.topics.forEach { t -> Chip(t, if (t == selectedNode) J.forest else J.inkFaint) }
                        }
                    }
                }
            }
        }
    }
}
