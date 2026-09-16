package com.beril.kaomoji.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
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
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.defaultWeight
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.beril.kaomoji.R
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

// ── Nixie tüp paleti — widget bir laboratuvar sayacı gibi görünür ──────
private val NixieGlass = Color(0xFF0B0710)
private val NixieFrame = Color(0xFF352745)
private val NixieAmber = Color(0xFFFFA23C)
private val NixieRed = Color(0xFFE12A44)
private val NixieWhite = Color(0xFFF3EEFA)
private val NixieViolet = Color(0xFF9D5CFF)

/**
 * Geniş (x ekseni) ve alçak (y ekseni) tek satırlık düzen: solda gerçek bir
 * nixie tüp illüstrasyonu (cam gövde + kehribar tel-çerçeve katot), sağında
 * bugünün görevi ve iki eylem. Yükseklik tek satıra sığacak şekilde sınırlı.
 */
@Composable
private fun MissionWidgetContent(context: Context) {
    val store = Store(context)
    val mission = MissionEngine.pick(store)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(ColorProvider(NixieFrame))
            .padding(1.5.dp)
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(NixieGlass))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.drawable.nixie_tube_illustration),
                contentDescription = null,
                modifier = GlanceModifier.width(28.dp).fillMaxHeight()
            )

            Spacer(GlanceModifier.width(10.dp))

            Column(modifier = GlanceModifier.defaultWeight()) {
                Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                    Box(
                        modifier = GlanceModifier
                            .size(5.dp)
                            .background(ColorProvider(NixieRed))
                    ) {}
                    Spacer(GlanceModifier.width(5.dp))
                    Text(
                        mission.title,
                        maxLines = 1,
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(NixieWhite)
                        )
                    )
                }
            }

            Spacer(GlanceModifier.width(8.dp))

            Text(
                "✓",
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(NixieWhite)
                ),
                modifier = GlanceModifier
                    .background(ColorProvider(NixieViolet))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .clickable(
                        actionRunCallback<CompleteMissionAction>(
                            actionParametersOf(TASK_ID_KEY to (mission.taskId ?: ""))
                        )
                    )
            )
            Spacer(GlanceModifier.width(6.dp))
            Text(
                "AÇ →",
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(NixieAmber)
                ),
                modifier = GlanceModifier
                    .padding(horizontal = 4.dp, vertical = 6.dp)
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
