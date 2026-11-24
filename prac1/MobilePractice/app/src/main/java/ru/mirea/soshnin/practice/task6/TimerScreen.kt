package ru.mirea.soshnin.practice.task6

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.mirea.soshnin.practice.ui.AskNotificationPermission
import ru.mirea.soshnin.practice.ui.TaskScaffold

@Composable
fun TimerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("30") }
    val left by TimerService.left.collectAsState()
    AskNotificationPermission()

    TaskScaffold("Задание 6. Background service", onBack) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.filter(Char::isDigit) },
            label = { Text("Секунды") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val intent = Intent(context, TimerService::class.java)
                    .putExtra(
                        TimerService.EXTRA_SECONDS,
                        text.toIntOrNull() ?: 0
                    )
                context.startService(intent)
            },
            enabled = left == 0 && text.isNotEmpty()
        ) { Text("Запустить таймер") }
        Spacer(Modifier.height(32.dp))
        if (left > 0) {
            Text("Осталось: $left с", fontSize = 32.sp)
        }
    }
}
