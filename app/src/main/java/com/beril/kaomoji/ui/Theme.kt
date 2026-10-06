package com.beril.kaomoji.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── "Genç Araştırmacı" paleti — Glow-Up uygulamasıyla birebir aynı.
// Kirli bordo-kızıl (ana vurgu) + soluk mor-eflatun (ikincil vurgu) + kirli
// haki/bej (zemin) + kırık beyaz (kart yüzeyi). İsim/yüz/replik yok — sadece
// renk/motif seviyesinde bir esinlenme. VOIDLAB'ın koyu/mor/kırmızı paletinin
// yerini alır; kasıtlı olarak daha az renk kullanır (minimalist).
object J {
    val paper = Color(0xFFD8CFB0)        // jacket_khaki_light — ana zemin
    val paperDeep = Color(0xFFA69B7C)    // jacket_khaki_dark — ikincil zemin / track
    val card = Color(0xFFF5F0E6)         // shirt_offwhite — kart yüzeyi
    val ink = Color(0xFF2B2520)          // ink_dark
    val inkSoft = Color(0xFF5A5348)      // ink_muted
    val inkFaint = Color(0xFF8B8371)     // ink_muted'den daha soluk

    val forest = Color(0xFF6E2430)       // tie_burgundy — ana vurgu / birincil eylem
    val apple = Color(0xFF8A7CA8)        // eye_lavender — ikincil vurgu
    val mint = Color(0xFF8A7CA8)         // eye_lavender (çağrı noktalarında alpha ile kullanılır)
    val lime = Color(0xFFAEA2C6)         // eye_lavender_light — vurgu arka planı
    val cherry = Color(0xFF4A1820)       // tie_burgundy_dark — uyarı / güçlü vurgu
    val berry = Color(0xFF4A1820)        // tie_burgundy_dark
    val blush = Color(0xFF8C3A46)        // tie_burgundy_light — sıcak vurgu arka planı
    val butter = Color(0xFFA9843F)       // ölçülü hardal/altın — uyarı vurgusu
    val bark = Color(0xFF6B5F87)         // eye_lavender_dark
    val sky = Color(0xFF8A7CA8)          // eye_lavender
    val lilac = Color(0xFF6B5F87)        // eye_lavender_dark — Feynman/tekrar vurgusu

    val line = Color(0xFFB0A47E)         // divider
    val lineSoft = Color(0xFFC7BC98)     // daha soluk divider
}

// ── Typography — "tez klasörü / araştırmacı defteri" hissi: başlıklarda
// ince akademik serif, gövdede sade sans-serif; monospace SADECE sayı/
// istatistik alanlarında (Tiny, Mono) kullanılır — VOIDLAB'ın her yeri
// monospace yapan "konsol" hissinden kasıtlı olarak uzaklaşır (minimalist).
val Display = TextStyle(
    fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
    fontSize = 26.sp, lineHeight = 32.sp, color = J.ink, letterSpacing = 0.2.sp
)
val TitleL = TextStyle(
    fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
    fontSize = 20.sp, lineHeight = 26.sp, color = J.ink
)
val TitleM = TextStyle(
    fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp, lineHeight = 22.sp, color = J.ink
)
val Body = TextStyle(
    fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,
    fontSize = 14.sp, lineHeight = 21.sp, color = J.ink
)
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

private val AcademicScheme = lightColorScheme(
    primary = J.forest,
    onPrimary = Color.White,
    secondary = J.apple,
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
fun LabTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AcademicScheme,
        typography = Typography(
            bodyLarge = Body, bodyMedium = Body, bodySmall = Small,
            titleLarge = TitleL, titleMedium = TitleM, labelSmall = Tiny
        ),
        content = content
    )
}

// ── Decorative building blocks ──────────────────────────────────────

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
