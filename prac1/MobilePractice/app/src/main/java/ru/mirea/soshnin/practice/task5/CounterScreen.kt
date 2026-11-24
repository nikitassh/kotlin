package ru.mirea.soshnin.practice.task5

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import ru.mirea.soshnin.practice.ui.AskNotificationPermission
import ru.mirea.soshnin.practice.ui.TaskScaffold

@Composable
fun CounterScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val seconds by CounterService.seconds.collectAsState()
    val running by CounterService.running.collectAsState()
    val intent = Intent(context, CounterService::class.java)
    AskNotificationPermission()

    TaskScaffold("Задание 5. Foreground service", onBack) {
        Spacer(Modifier.height(48.dp))
        Text("$seconds", fontSize = 96.sp)
        Text(if (running) "Сервис работает" else "Сервис остановлен")
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = {
                    ContextCompat.startForegroundService(context, intent)
                },
                enabled = !running
            ) { Text("Старт") }
            OutlinedButton(
                onClick = { context.stopService(intent) },
                enabled = running
            ) { Text("Стоп") }
        }
    }
}
