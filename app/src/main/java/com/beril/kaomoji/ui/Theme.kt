package com.beril.kaomoji.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── VOIDLAB palette — laboratuvar karanlığı, mor devre ışığı, nixie kırmızısı ──
object J {
    val paper = Color(0xFF0B0710)        // void — near-black, faint violet undertone
    val paperDeep = Color(0xFF140C1E)
    val card = Color(0xFF1B1228)
    val ink = Color(0xFFF3EEFA)          // chalk white
    val inkSoft = Color(0xFFB6ABC9)      // muted lavender-gray
    val inkFaint = Color(0xFF6D6280)     // faint violet-gray

    val apple = Color(0xFF9D5CFF)        // electric violet — primary
    val forest = Color(0xFF6425B8)       // deep violet — primary (pressed/strong)
    val mint = Color(0xFFDCCBFA)         // pale violet glow
    val lime = Color(0xFFC7B4EF)         // soft lavender
    val cherry = Color(0xFFE12A44)       // signal red — secondary / error
    val berry = Color(0xFFB4102E)        // deep crimson
    val blush = Color(0xFFCE6E8C)        // muted rose
    val butter = Color(0xFFFFA23C)       // nixie amber glow — warnings / highlights
    val bark = Color(0xFF43223A)         // dark plum-brown
    val sky = Color(0xFF7C86E0)          // cool blue-violet
    val lilac = Color(0xFFAE8CFB)        // violet accent

    val line = Color(0xFF352745)         // dark violet-gray border
    val lineSoft = Color(0xFF241A33)
}

fun subjectColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    J.apple
}

// ── Typography — laboratuvar konsolu: monospace başlıklar, sade gövde ─
val Display = TextStyle(
    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
    fontSize = 26.sp, lineHeight = 32.sp, color = J.ink, letterSpacing = 0.4.sp
)
val TitleL = TextStyle(
    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
    fontSize = 20.sp, lineHeight = 26.sp, color = J.ink, letterSpacing = 0.3.sp
)
val TitleM = TextStyle(
    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp, lineHeight = 22.sp, color = J.ink
)
val Body = TextStyle(
    fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,
    fontSize = 14.sp, lineHeight = 21.sp, color = J.ink
)
val BodySoft = Body.copy(color = J.inkSoft)
val Small = TextStyle(
    fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,
    fontSize = 12.sp, lineHeight = 17.sp, color = J.inkSoft
)
val Tiny = TextStyle(
    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium,
    fontSize = 10.sp, lineHeight = 14.sp, color = J.inkFaint
)
val Mono = TextStyle(
    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium,
    fontSize = 12.sp, color = J.inkSoft
)

private val VoidScheme = darkColorScheme(
    primary = J.apple,
    onPrimary = Color.White,
    secondary = J.cherry,
    onSecondary = Color.White,
    background = J.paper,
    onBackground = J.ink,
    surface = J.card,
    onSurface = J.ink,
    surfaceVariant = J.paperDeep,
    onSurfaceVariant = J.inkSoft,
    outline = J.line,
    error = J.cherry
)

@Composable
fun KaomojiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VoidScheme,
        typography = Typography(
            bodyLarge = Body, bodyMedium = Body, bodySmall = Small,
            titleLarge = TitleL, titleMedium = TitleM, labelSmall = Tiny
        ),
        content = content
    )
}

// ── Decorative building blocks ──────────────────────────────────────

/** Faint schematic grid — laboratuvar defteri / osiloskop ızgarası. */
fun Modifier.gingham(
    color: Color = J.apple.copy(alpha = 0.05f),
    cell: Float = 26f
): Modifier = this.drawBehind {
    var x = 0f
    while (x < size.width) {
        drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        x += cell
    }
    var y = 0f
    while (y < size.height) {
        drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += cell
    }
}

/** Dashed containment-panel border — hücre/sinyal çerçevesi. */
fun Modifier.dashed(
    color: Color = J.line,
    width: Float = 1.4f,
    radius: Float = 14f
): Modifier = this.drawBehind {
    drawRoundRect(
        color = color,
        style = Stroke(
            width = width,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f), 0f)
        ),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
    )
}

@Composable
fun Card(
    modifier: Modifier = Modifier,
    bg: Color = J.card,
    border: Color = J.line,
    radius: Int = 18,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(radius.dp))
            .border(1.dp, border, RoundedCornerShape(radius.dp))
            .padding(14.dp),
        content = content
    )
}

@Composable
fun Chip(
    text: String,
    color: Color = J.forest,
    bg: Color = color.copy(alpha = 0.12f),
    modifier: Modifier = Modifier
) {
    Text(
        text,
        style = Tiny.copy(color = color, fontWeight = FontWeight.SemiBold),
        modifier = modifier
            .background(bg, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
fun Sticker(emoji: String, size: Int = 34, bg: Color = J.lime.copy(alpha = 0.35f)) {
    Box(
        Modifier
            .size(size.dp)
            .background(bg, RoundedCornerShape(50)),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) { Text(emoji, style = TextStyle(fontSize = (size * 0.5).sp)) }
}

@Composable
fun Bar(progress: Float, color: Color = J.apple, height: Int = 6, track: Color = J.paperDeep) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(height.dp)
            .background(track, RoundedCornerShape(50))
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(color, RoundedCornerShape(50))
        )
    }
}

@Composable
fun SectionLabel(text: String, emoji: String? = null) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        if (emoji != null) {
            Text(emoji, style = TextStyle(fontSize = 13.sp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text.uppercase(),
            style = Tiny.copy(
                color = J.inkSoft,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
            )
        )
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(J.lineSoft)
        )
    }
}

@Composable
fun Empty(emoji: String, text: String, sub: String? = null) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 34.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Text(emoji, style = TextStyle(fontSize = 34.sp))
        Spacer(Modifier.height(8.dp))
        Text(text, style = TitleM.copy(color = J.inkSoft))
        if (sub != null) {
            Spacer(Modifier.height(4.dp))
            Text(sub, style = Small)
        }
    }
}
