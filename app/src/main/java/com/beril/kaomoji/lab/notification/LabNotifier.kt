package com.beril.kaomoji.lab.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.beril.kaomoji.MainActivity
import com.beril.kaomoji.R
import com.beril.kaomoji.lab.repository.LabRepository

/**
 * Kilit ekranı entegrasyonu (§44) — Android gerçek kilit ekranı widget'larını desteklemediği
 * için görünürlüğü PUBLIC, kalıcı (ongoing) bir bildirim kullanılıyor. İçerik `LabWidget` ile
 * aynı veriye bakar (vadesi gelen kart sayısı + en yakın/geçmiş hedef tarihli nesne) — iki ayrı
 * sorgu yazılmadı, aynı `LabRepository` metodları paylaşılıyor.
 */
object LabNotifier {
    private const val CHANNEL_ID = "lab_home"
    private const val NOTIF_ID = 4242

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(NotificationManager::class.java)
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notif_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = context.getString(R.string.notif_channel_desc)
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                    setShowBadge(false)
                }
                mgr.createNotificationChannel(ch)
            }
        }
    }

    /** Güncel durumu okuyup bildirimi tazeler — `LabWidget` ile aynı iki sorgu. */
    suspend fun refresh(context: Context) {
        ensureChannel(context)
        val repo = LabRepository(context)
        val overdue = repo.pastTargetDate().firstOrNull()
        val next = repo.upcoming(days = 30).firstOrNull()
        val dueCount = repo.dueFlashcards().size

        val title = when {
            overdue != null -> "⚠️ ${overdue.title}"
            next != null -> "📋 ${next.title}"
            dueCount > 0 -> "🃏 $dueCount kart vadesi geldi"
            else -> "🧪 Lab"
        }
        val text = when {
            overdue != null -> "hedef tarihi geçti — seçenek, hata değil"
            next != null && dueCount > 0 -> "$dueCount kart vadesi geldi"
            next != null -> "yaklaşıyor"
            dueCount > 0 -> "gösterim için hazır"
            else -> "şu an önemli olan bir şey yok"
        }

        val openIntent = Intent(context, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF6E2430.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openPending)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID, builder.build())
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS izni verilmemiş — sessizce geç
        }
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIF_ID)
    }
}
