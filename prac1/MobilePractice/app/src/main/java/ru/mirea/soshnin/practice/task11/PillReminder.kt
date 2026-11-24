package ru.mirea.soshnin.practice.task11

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import ru.mirea.soshnin.practice.Notifications
import java.util.Calendar

// Задание 11. Будильник на 20:00 каждый день
object PillScheduler {
    const val HOUR = 20
    private const val PREFS = "pill"
    private const val KEY_ON = "enabled"

    fun isEnabled(context: Context) =
        context.getSharedPreferences(PREFS, 0)
            .getBoolean(KEY_ON, false)

    private fun setEnabled(context: Context, on: Boolean) =
        context.getSharedPreferences(PREFS, 0).edit()
            .putBoolean(KEY_ON, on).apply()

    // ближайшие 20:00 - сегодня или завтра
    fun nextTime(now: Calendar = Calendar.getInstance()): Calendar {
        val next = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!next.after(now)) next.add(Calendar.DAY_OF_YEAR, 1)
        return next
    }

    private fun pendingIntent(context: Context) =
        PendingIntent.getBroadcast(
            context, 11,
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

    fun schedule(context: Context) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        val time = nextTime().timeInMillis
        val pi = pendingIntent(context)
        if (Build.VERSION.SDK_INT >= 31 &&
            !alarm.canScheduleExactAlarms()
        ) {
            // без разрешения на точные будильники - обычный
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi)
        } else {
            alarm.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, time, pi
            )
        }
    }

    fun enable(context: Context) {
        setEnabled(context, true)
        schedule(context)
    }

    fun disable(context: Context) {
        setEnabled(context, false)
        context.getSystemService(AlarmManager::class.java)
            .cancel(pendingIntent(context))
    }
}

// Срабатывание будильника
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val b = Notifications
            .builder(context, Notifications.CHANNEL_REMINDER)
            .setContentTitle("Напоминание")
            .setContentText("Время принять таблетку!")
            .setAutoCancel(true)
        Notifications.show(context, 11, b)
        // ставим будильник на следующий день
        if (PillScheduler.isEnabled(context)) {
            PillScheduler.schedule(context)
        }
    }
}

// После перезагрузки все будильники сбрасываются
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED &&
            PillScheduler.isEnabled(context)
        ) {
            PillScheduler.schedule(context)
        }
    }
}
