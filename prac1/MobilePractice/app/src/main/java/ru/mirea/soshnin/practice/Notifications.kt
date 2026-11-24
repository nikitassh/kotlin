package ru.mirea.soshnin.practice

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object Notifications {
    const val CHANNEL_SERVICES = "services"
    const val CHANNEL_WORK = "work"
    const val CHANNEL_REMINDER = "reminder"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(
            NotificationManager::class.java
        )
        listOf(
            NotificationChannel(
                CHANNEL_SERVICES, "Сервисы",
                NotificationManager.IMPORTANCE_LOW
            ),
            NotificationChannel(
                CHANNEL_WORK, "Фоновые задачи",
                NotificationManager.IMPORTANCE_LOW
            ),
            NotificationChannel(
                CHANNEL_REMINDER, "Напоминания",
                NotificationManager.IMPORTANCE_HIGH
            )
        ).forEach { manager.createNotificationChannel(it) }
    }

    fun builder(context: Context, channel: String) =
        NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_launcher_foreground)

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    fun show(context: Context, id: Int, b: NotificationCompat.Builder) {
        if (canPost(context)) {
            try {
                NotificationManagerCompat.from(context)
                    .notify(id, b.build())
            } catch (_: SecurityException) {
            }
        }
    }
}
