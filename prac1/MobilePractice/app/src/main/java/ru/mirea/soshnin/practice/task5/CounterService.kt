package ru.mirea.soshnin.practice.task5

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.mirea.soshnin.practice.Notifications

// Задание 5. Foreground-сервис, считает секунды
class CounterService : Service() {

    companion object {
        const val NOTIFICATION_ID = 5
        private val _seconds = MutableStateFlow(0)
        val seconds = _seconds.asStateFlow()
        val running = MutableStateFlow(false)
    }

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default
    )
    private var job: Job? = null

    override fun onStartCommand(
        intent: Intent?, flags: Int, startId: Int
    ): Int {
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, notification(_seconds.value),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
        if (job == null) {
            _seconds.value = 0
            running.value = true
            job = scope.launch {
                while (isActive) {
                    delay(1000)
                    _seconds.value++
                    Notifications.show(
                        this@CounterService, NOTIFICATION_ID,
                        builder(_seconds.value)
                    )
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun builder(sec: Int) =
        Notifications.builder(this, Notifications.CHANNEL_SERVICES)
            .setContentTitle("Счетчик времени")
            .setContentText("Прошло $sec секунд")
            .setOngoing(true)
            .setOnlyAlertOnce(true)

    private fun notification(sec: Int) = builder(sec).build()

    override fun onDestroy() {
        scope.cancel()
        running.value = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
