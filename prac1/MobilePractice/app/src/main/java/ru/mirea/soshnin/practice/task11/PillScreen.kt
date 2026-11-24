package ru.mirea.soshnin.practice.task11

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.mirea.soshnin.practice.ui.AskNotificationPermission
import ru.mirea.soshnin.practice.ui.TaskScaffold
import java.util.Calendar

fun nextText(): String {
    val now = Calendar.getInstance()
    val next = PillScheduler.nextTime(now)
    val day = if (next.get(Calendar.DAY_OF_YEAR) ==
        now.get(Calendar.DAY_OF_YEAR)
    ) "сегодня" else "завтра"
    return "Следующее напоминание: $day в ${PillScheduler.HOUR}:00"
}

@Composable
fun PillScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var enabled by remember {
        mutableStateOf(PillScheduler.isEnabled(context))
    }
    AskNotificationPermission()

    TaskScaffold("Задание 11. Таблетка", onBack) {
        Spacer(Modifier.weight(1f))
        Text(
            "Напоминание о таблетке",
            fontSize = 30.sp,
            textAlign = TextAlign.Center
        )
        Text("каждый день в ${PillScheduler.HOUR}:00", fontSize = 20.sp)
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(20.dp)
                    .background(
                        if (enabled) Color(0xFF2E7D32) else Color.Gray,
                        CircleShape
                    )
            )
            Spacer(Modifier.width(8.dp))
            Text(if (enabled) "Включено" else "Выключено", fontSize = 20.sp)
        }
        if (enabled) {
            Spacer(Modifier.height(12.dp))
            Text(nextText(), fontSize = 18.sp)
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                if (enabled) {
                    PillScheduler.disable(context)
                } else {
                    PillScheduler.enable(context)
                }
                enabled = !enabled
            },
            modifier = Modifier.fillMaxWidth().height(64.dp)
        ) {
            Text(
                if (enabled) "Выключить напоминание"
                else "Включить напоминание",
                fontSize = 18.sp
            )
        }
        Spacer(Modifier.weight(1f))
        // для проверки: вызвать приемник сразу
        TextButton(onClick = {
            context.sendBroadcast(
                Intent(context, ReminderReceiver::class.java)
            )
        }) { Text("Проверить уведомление") }
    }
}
