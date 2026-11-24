package ru.mirea.soshnin.network.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val Deep = Color(0xFF5E35B1)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Deep), content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}

// Общие состояния экрана для всех сетевых заданий
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Composable
fun <T> StateContent(
    state: UiState<T>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    when (state) {
        UiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is UiState.Error -> Column(
            modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(state.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Повторить") }
        }
        is UiState.Success -> content(state.data)
    }
}

// Понятный текст ошибки для пользователя
fun Throwable.toUserMessage(): String = when (this) {
    is java.net.UnknownHostException, is java.net.ConnectException ->
        "Нет соединения с сервером"
    is java.net.SocketTimeoutException -> "Сервер не отвечает"
    else -> message ?: "Неизвестная ошибка"
}

data class TaskItem(val number: String, val title: String, val open: Int)

val tasks = listOf(
    TaskItem("Задание 1", "Фотокаталог (Retrofit)", 1),
    TaskItem("Задание 2", "Нобелевские лауреаты (Ktor Client)", 2),
    TaskItem("Задание 3", "Авторизация (dummyjson)", 3),
    TaskItem("Задание 6", "Нобелевские лауреаты: свой сервер", 6),
    TaskItem("Задание 7", "BLE: монитор пульса", 7)
)

@Composable
fun MenuScreen(onOpen: (Int) -> Unit) {
    Scaffold(topBar = { AppBar("Модуль 6. Сеть и BLE") }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tasks) { task ->
                Card(Modifier.fillMaxWidth().clickable { onOpen(task.open) }) {
                    ListItem(
                        overlineContent = { Text(task.number) },
                        headlineContent = { Text(task.title) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
            item {
                Text(
                    "Задания 4–5 – серверная часть (проект NobelServer)",
                    color = Color.Gray,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

// Логи сетевых запросов Ktor в logcat (тег KtorHttp)
object AndroidHttpLogger : io.ktor.client.plugins.logging.Logger {
    override fun log(message: String) {
        android.util.Log.i("KtorHttp", message)
    }
}
