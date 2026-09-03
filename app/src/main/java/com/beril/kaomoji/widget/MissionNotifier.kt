package com.beril.kaomoji.widget

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.glance.appwidget.updateAll
import com.beril.kaomoji.MainActivity
import com.beril.kaomoji.R
import com.beril.kaomoji.data.MissionEngine
import com.beril.kaomoji.data.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * "Kilit ekranı entegrasyonu": Android artık gerçek kilit ekranı widget'larını
 * desteklemiyor (5.0'dan beri kaldırıldı). Bunun yerine görünürlüğü PUBLIC,
 * kalıcı (ongoing) bir bildirim kullanıyoruz — bu, cihaz kilitliyken de
 * ekranda görünür ve kilidi açmadan "Tamamla" ile dokunulabilir.
 */
object MissionNotifier {
    private const val CHANNEL_ID = "mission_lockscreen"
    const val NOTIF_ID = 4242
    const val ACTION_COMPLETE = "com.beril.kaomoji.ACTION_COMPLETE_MISSION"
    const val EXTRA_TASK_ID = "task_id"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(NotificationManager::class.java)
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notif_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = context.getString(R.string.notif_channel_desc)
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                    setShowBadge(false)
                }
                mgr.createNotificationChannel(ch)
            }
        }
    }

    /** Mevcut görevi okuyup bildirimi tazeler. Widget'la aynı MissionEngine'i kullanır. */
    fun refresh(context: Context) {
        ensureChannel(context)
        val store = Store(context)
        val mission = MissionEngine.pick(store)

        val openIntent = Intent(context, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(context, CompleteMissionReceiver::class.java).apply {
            action = ACTION_COMPLETE
            putExtra(EXTRA_TASK_ID, mission.taskId)
        }
        val completePending = PendingIntent.getBroadcast(
            context, 1, completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF9D5CFF.toInt())
            .setContentTitle("${mission.emoji} ${mission.title}")
            .setContentText(mission.why)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mission.why))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openPending)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (!mission.taskId.isNullOrBlank()) {
            builder.addAction(0, "✓ Tamamla", completePending)
        }

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID, builder.build())
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS izni verilmemiş — sessizce geç, ana ekranda tekrar istenecek
        }
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIF_ID)
    }
}

/** Bildirimdeki "Tamamla" aksiyonu — uygulamayı açmadan görevi işaretler. */
class CompleteMissionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != MissionNotifier.ACTION_COMPLETE) return
        val taskId = intent.getStringExtra(MissionNotifier.EXTRA_TASK_ID)
        val appCtx = context.applicationContext

        if (!taskId.isNullOrBlank()) {
            val store = Store(appCtx)
            store.toggleTask(taskId)
        }
        MissionNotifier.refresh(appCtx)

        // Glance widget'ları da tazele (widget haricinde tetiklenen tek yer burası)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                MissionWidget().updateAll(appCtx)
            } catch (_: Exception) {
            }
        }
    }
}
