package com.beril.kaomoji.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beril.kaomoji.data.Curriculum
import com.beril.kaomoji.data.Task

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
            .clickable(enabled = enabled) { onClick() }
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
            .clickable { onClick() }
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
    Box(
        modifier
            .fillMaxWidth()
            .background(J.paper, RoundedCornerShape(12.dp))
            .border(1.dp, J.line, RoundedCornerShape(12.dp))
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
fun Checkbox(checked: Boolean, onToggle: () -> Unit, color: Color = J.forest) {
    Box(
        Modifier
            .size(22.dp)
            .background(if (checked) color else Color.Transparent, RoundedCornerShape(7.dp))
            .border(1.5.dp, if (checked) color else J.line, RoundedCornerShape(7.dp))
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        if (checked) Text("✓", style = TextStyle(fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold))
    }
}

@Composable
fun TaskRow(
    task: Task,
    c: Curriculum,
    done: Boolean,
    skipped: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    onSkip: (() -> Unit)? = null,
    onExplain: (() -> Unit)? = null
) {
    val sd = c.subject(task.subject)
    val kd = c.kind(task.kind)
    val col = subjectColor(sd?.color ?: "#7BB661")

    Row(
        Modifier
            .fillMaxWidth()
            .background(
                if (done) J.paperDeep.copy(alpha = 0.5f) else J.card,
                RoundedCornerShape(14.dp)
            )
            .border(
                1.dp,
                if (done) J.lineSoft else col.copy(alpha = 0.30f),
                RoundedCornerShape(14.dp)
            )
            .padding(11.dp),
        verticalAlignment = Alignment.Top
    ) {
        Checkbox(done, { if (enabled) onToggle() }, col)
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Chip("${sd?.emoji ?: ""} ${sd?.name ?: task.subject}", col)
                Spacer(Modifier.width(5.dp))
                Chip("${kd?.emoji ?: ""} ${kd?.name ?: ""}", J.inkSoft, J.paperDeep)
                Spacer(Modifier.width(5.dp))
                Text("${task.minutes}′", style = Tiny)
            }
            Spacer(Modifier.height(5.dp))
            Text(
                task.text,
                style = if (done)
                    Body.copy(color = J.inkFaint, textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                else Body
            )
            if (skipped && !done) {
                Spacer(Modifier.height(4.dp))
                Text("atlandı", style = Tiny.copy(color = J.butter))
            }
            if (enabled && !done && (onSkip != null || onExplain != null)) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    if (onExplain != null && task.kind == "explain")
                        GhostBtn("Kaydet", onExplain, emoji = "🎙️")
                    if (onSkip != null) GhostBtn("Atla", onSkip, color = J.inkFaint)
                }
            }
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

@Composable
fun Sheet(title: String, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(J.paper, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = TitleL, modifier = Modifier.weight(1f))
            Text(
                "✕",
                style = TextStyle(fontSize = 18.sp, color = J.inkSoft),
                modifier = Modifier
                    .clickable { onClose() }
                    .padding(6.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        content()
        Spacer(Modifier.height(10.dp))
    }
}
