package com.beril.kaomoji.lab.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.unit.dp
import com.beril.kaomoji.lab.repository.LabRepository

/**
 * Uygulamanın ana ekran widget'ı — vadesi gelen kart sayısı + en yakın sınav/ödev. §43'ün
 * "widget da görev yöneticisine indirgenmesin, ama yararlı olsun" ilkesiyle, tek bir sayı/
 * satır gösterir.
 */
class LabWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = LabRepository(context)
        val dueCount = repo.dueFlashcards().size
        val nextExam = repo.upcoming(days = 30).firstOrNull()?.title

        provideContent {
            GlanceTheme {
                LabWidgetContent(dueCount, nextExam)
            }
        }
    }
}

class LabWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LabWidget()
}

private val LabBg = Color(0xFF2B2520)
private val LabAccent = Color(0xFFAEA2C6)
private val LabText = Color(0xFFF5F0E6)

@Composable
private fun LabWidgetContent(dueCount: Int, nextExam: String?) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(ColorProvider(LabBg))
            .padding(10.dp)
            .clickable(androidx.glance.appwidget.action.actionRunCallback<OpenAppAction>()),
        horizontalAlignment = Alignment.Horizontal.Start,
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(
            "🧪 Lab",
            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ColorProvider(LabAccent)),
        )
        Text(
            if (dueCount > 0) "$dueCount kart vadesi geldi" else "Vadesi gelen kart yok",
            style = TextStyle(fontSize = 13.sp, color = ColorProvider(LabText)),
        )
        if (nextExam != null) {
            Text(
                "Yaklaşan: $nextExam",
                style = TextStyle(fontSize = 11.sp, color = ColorProvider(LabText)),
                maxLines = 1,
            )
        }
    }
}

class OpenAppAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        intent?.let { context.startActivity(it) }
    }
}
