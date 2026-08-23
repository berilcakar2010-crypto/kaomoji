package com.beril.kaomoji.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.beril.kaomoji.data.MissionEngine
import com.beril.kaomoji.data.Store

/**
 * Ana ekran widget'ı. Bugünün küçük görevini gösterir, "Tamamla" butonu görevi
 * store'da işaretler ve hem widget'ı hem kilit ekranı bildirimini tazeler.
 *
 * Zamansız felsefeyle aynı çizgide: widget da "bugün ne çalışman lazım" demez,
 * "sırada anlamlı olan tek şey bu" der.
 */
class MissionWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                MissionWidgetContent(context)
            }
        }
    }
}

class MissionWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MissionWidget()
}

@Composable
private fun MissionWidgetContent(context: Context) {
    val store = Store(context)
    val mission = MissionEngine.pick(store)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(ColorProvider(Color(0xFFF6F1E4)))
            .padding(14.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
            Text(
                mission.emoji,
                style = TextStyle(fontSize = 20.sp)
            )
            Spacer(GlanceModifier.width(6.dp))
            Text(
                "bugünün küçük görevi",
                style = TextStyle(fontSize = 11.sp, color = ColorProvider(Color(0xFF9A9081)))
            )
        }

        Spacer(GlanceModifier.height(6.dp))

        Text(
            mission.title,
            maxLines = 3,
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = ColorProvider(Color(0xFF2B2620))
            )
        )

        Spacer(GlanceModifier.height(10.dp))

        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                "✓ Tamamla",
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(Color(0xFFFFFFFF))
                ),
                modifier = GlanceModifier
                    .background(ColorProvider(Color(0xFF3F6B4A)))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .clickable(
                        actionRunCallback<CompleteMissionAction>(
                            actionParametersOf(TASK_ID_KEY to (mission.taskId ?: ""))
                        )
                    )
            )
            Spacer(GlanceModifier.width(8.dp))
            Text(
                "Aç →",
                style = TextStyle(
                    fontSize = 13.sp,
                    color = ColorProvider(Color(0xFF6B6154))
                ),
                modifier = GlanceModifier
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .clickable(actionRunCallback<OpenAppAction>())
            )
        }
    }
}

val TASK_ID_KEY = ActionParameters.Key<String>("task_id")

class CompleteMissionAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[TASK_ID_KEY]
        if (!taskId.isNullOrBlank()) {
            val store = Store(context)
            store.toggleTask(taskId)
        }
        MissionWidget().updateAll(context)
        MissionNotifier.refresh(context)
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
