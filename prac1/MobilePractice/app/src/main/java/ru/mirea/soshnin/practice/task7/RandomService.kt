package ru.mirea.soshnin.practice.task7

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

// Задание 7. Привязанный сервис: случайное число раз в секунду
class RandomService : Service() {

    inner class LocalBinder : Binder() {
        fun getService() = this@RandomService
    }

    private val binder = LocalBinder()
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default
    )
    private val _number = MutableStateFlow<Int?>(null)
    val number = _number.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            while (isActive) {
                _number.value = Random.nextInt(0, 101)
                delay(1000)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    // сервис живет, пока к нему кто-то привязан
    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
