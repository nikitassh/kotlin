package ru.mirea.soshnin.practice.task6

import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import ru.mirea.soshnin.practice.Notifications

// Задание 6. Обычный (фоновый) сервис: одноразовый таймер
class TimerService : Service() {

    companion object {
        const val EXTRA_SECONDS = "seconds"
        // сколько осталось, для отображения на экране
        val left = MutableStateFlow(0)
    }

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default
    )

    override fun onStartCommand(
        intent: Intent?, flags: Int, startId: Int
    ): Int {
        val seconds = intent?.getIntExtra(EXTRA_SECONDS, 0) ?: 0
        scope.launch {
            for (s in seconds downTo 1) {
                left.value = s
                delay(1000)
            }
            left.value = 0
            showDone()
            // сервис сам себя останавливает
            stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    private fun showDone() {
        val b = Notifications
            .builder(this, Notifications.CHANNEL_REMINDER)
            .setContentTitle("Таймер завершен!")
            .setContentText("Время вышло")
            .setAutoCancel(true)
        Notifications.show(this, 6, b)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
