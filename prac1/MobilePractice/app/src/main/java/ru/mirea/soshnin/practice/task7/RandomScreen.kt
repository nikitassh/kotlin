package ru.mirea.soshnin.practice.task7

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ru.mirea.soshnin.practice.ui.TaskScaffold

@Composable
fun RandomScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var service by remember { mutableStateOf<RandomService?>(null) }
    val empty = remember { MutableStateFlow<Int?>(null) }
    val flow: StateFlow<Int?> = service?.number ?: empty
    val number by flow.collectAsState()

    val connection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?, binder: IBinder?
            ) {
                service = (binder as RandomService.LocalBinder)
                    .getService()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                service = null
            }
        }
    }

    fun disconnect() {
        if (service != null) {
            context.unbindService(connection)
            service = null
        }
    }

    // при уходе с экрана отвязываемся
    DisposableEffect(Unit) { onDispose { disconnect() } }

    TaskScaffold("Задание 7. Bound service", onBack) {
        Spacer(Modifier.height(48.dp))
        Text(
            if (service != null) "${number ?: "-"}" else "-",
            fontSize = 96.sp
        )
        Text(if (service != null) "Подключено" else "Отключено")
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = {
                    val intent = Intent(
                        context, RandomService::class.java
                    )
                    context.bindService(
                        intent, connection, Context.BIND_AUTO_CREATE
                    )
                },
                enabled = service == null
            ) { Text("Подключиться") }
            OutlinedButton(
                onClick = { disconnect() },
                enabled = service != null
            ) { Text("Отключиться") }
        }
    }
}
