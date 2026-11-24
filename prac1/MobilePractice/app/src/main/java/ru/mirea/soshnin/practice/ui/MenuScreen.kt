package ru.mirea.soshnin.practice.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class TaskItem(val number: Int, val title: String)

val tasks = listOf(
    TaskItem(3, "Поиск репозиториев GitHub"),
    TaskItem(4, "Социальная лента"),
    TaskItem(5, "Foreground service: счетчик"),
    TaskItem(6, "Background service: таймер"),
    TaskItem(7, "Bound service: случайные числа"),
    TaskItem(8, "WorkManager: обработка фото"),
    TaskItem(9, "WorkManager: прогноз погоды"),
    TaskItem(10, "Местоположение и адрес"),
    TaskItem(11, "Напоминание о таблетке"),
    TaskItem(12, "Факты о животных (Flow)"),
    TaskItem(13, "Курс валют (StateFlow)"),
    TaskItem(14, "Компас")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(onOpen: (Int) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Практики Kotlin") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tasks) { task ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(task.number) }
                ) {
                    ListItem(
                        overlineContent = {
                            Text("Задание ${task.number}")
                        },
                        headlineContent = { Text(task.title) },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            }
        }
    }
}
