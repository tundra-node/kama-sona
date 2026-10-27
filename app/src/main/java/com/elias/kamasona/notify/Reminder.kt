package com.elias.kamasona.notify

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.elias.kamasona.MainActivity
import com.elias.kamasona.R
import com.elias.kamasona.data.Store
import java.util.Calendar

/**
 * One daily nudge. Elias's stated gap was "remembering to do it", so this is
 * the app's whole reason for asking for a notification slot.
 */
object Reminder {

    const val CHANNEL = "kama_sona_daily"
    private const val REQ = 4242
    private const val HOUR = 19 // local 19:00

    // Android 12+ rejects PendingIntents that do not declare mutability.
    private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    fun ensureScheduled(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pending(ctx)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.timeInMillis, AlarmManager.INTERVAL_DAY, pi)
    }

    private fun pending(ctx: Context): PendingIntent {
        val i = Intent(ctx, ReminderReceiver::class.java)
        return PendingIntent.getBroadcast(ctx, REQ, i, FLAGS)
    }

    fun post(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CHANNEL, "tenpo sona", NotificationManager.IMPORTANCE_DEFAULT)
            ch.description = "daily toki pona reminder"
            nm.createNotificationChannel(ch)
        }
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            FLAGS
        )
        val text = (ctx.getSharedPreferences("kama_sona", Context.MODE_PRIVATE)
            .getString("today_word", null))?.let { "o kepeken e nimi \"$it\"" }
            ?: "o kama sona e toki pona"
        val n: Notification = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle("tenpo sona")
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        nm.notify(1, n)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val store = Store(context.applicationContext)
        val (reviews, sentences) = try { store.todayCounts() } finally { store.close() }
        if (reviews + sentences > 0) return // already practiced today
        Reminder.post(context)
    }
}
