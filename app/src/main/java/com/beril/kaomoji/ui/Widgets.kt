package com.beril.kaomoji.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beril.kaomoji.ui.nav.dpadFocusable

@Composable
fun Btn(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bg: Color = J.forest,
    fg: Color = Color.White,
    emoji: String? = null,
    enabled: Boolean = true
) {
    Row(
        modifier
            .background(if (enabled) bg else J.line, RoundedCornerShape(14.dp))
            .dpadFocusable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (emoji != null) {
            Text(emoji, style = TextStyle(fontSize = 14.sp))
            Spacer(Modifier.width(7.dp))
        }
        Text(
            text,
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) fg else J.inkFaint
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun GhostBtn(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emoji: String? = null,
    color: Color = J.inkSoft
) {
    Row(
        modifier
            .border(1.dp, J.line, RoundedCornerShape(14.dp))
            .dpadFocusable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (emoji != null) {
            Text(emoji, style = TextStyle(fontSize = 13.sp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = color))
    }
}

@Composable
fun Field(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    single: Boolean = false
) {
    // Fokus aldığında/kaybettiğinde kenarlık rengi yumuşak geçiş yapıyor, ve içerik satır
    // sayısı değiştiğinde (çok satırlı bir alan büyüdüğünde) yükseklik ani değil animasyonlu
    // değişiyor — §11/§38'in "küçük, tatmin edici hareket" ilkesini `Field` paylaşılan bileşeni
    // için kapatıyor (tek yerde eklendiği için bunu kullanan HER ekrana yayılıyor).
    var focused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = if (focused) J.forest else J.line,
        animationSpec = tween(150),
        label = "field-border",
    )
    Box(
        modifier
            .fillMaxWidth()
            .animateContentSize()
            .background(J.paper, RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        if (value.isEmpty()) Text(placeholder, style = Body.copy(color = J.inkFaint))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = Body,
            singleLine = single,
            cursorBrush = SolidColor(J.forest),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = (minLines * 21).dp)
                .onFocusChanged { focused = it.isFocused }
        )
    }
}

@Composable
fun Selector(
    options: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    labels: ((String) -> String)? = null
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(options.size) { i ->
            val o = options[i]
            val on = o == selected
            Text(
                labels?.invoke(o) ?: o,
                style = Small.copy(
                    color = if (on) Color.White else J.inkSoft,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal
                ),
                modifier = Modifier
                    .background(if (on) J.forest else J.paperDeep, RoundedCornerShape(50))
                    .clickable { onSelect(o) }
                    .padding(horizontal = 11.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun StatTile(value: String, label: String, emoji: String, color: Color = J.forest) {
    Column(
        Modifier
            .background(J.card, RoundedCornerShape(14.dp))
            .border(1.dp, J.line, RoundedCornerShape(14.dp))
            .padding(vertical = 11.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, style = TextStyle(fontSize = 15.sp))
        Spacer(Modifier.height(3.dp))
        Text(
            value,
            style = TextStyle(
                fontSize = 17.sp, fontWeight = FontWeight.Bold,
                color = color, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
            )
        )
        Text(label, style = Tiny, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

