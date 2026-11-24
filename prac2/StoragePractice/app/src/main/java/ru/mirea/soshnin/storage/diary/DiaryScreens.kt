package ru.mirea.soshnin.storage.diary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.mirea.soshnin.storage.ui.AppBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru"))

@Composable
fun DiaryApp(onExit: () -> Unit) {
    val vm: DiaryViewModel = viewModel()
    // null - список, NEW - новая запись, иначе имя файла
    var opened by rememberSaveable { mutableStateOf<String?>(null) }
    val entries by vm.entries.collectAsStateWithLifecycle()

    if (opened == null) {
        DiaryListScreen(vm, onExit, onOpen = { opened = it })
    } else {
        val entry = entries.find { it.fileName == opened }
        BackHandler { opened = null }
        DiaryEditScreen(
            entry = entry,
            onSave = { title, text ->
                vm.save(title, text, entry)
                opened = null
            },
            onBack = { opened = null }
        )
    }
}

@Composable
fun DiaryListScreen(
    vm: DiaryViewModel,
    onExit: () -> Unit,
    onOpen: (String) -> Unit
) {
    val entries by vm.entries.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { AppBar("Задание 1. Мой дневник", onExit) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onOpen(NEW) },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Новая запись") }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                entries.isEmpty() -> EmptyDiary(Modifier.align(Alignment.Center))
                else -> LazyColumn(
                    contentPadding = PaddingValues(12.dp, 12.dp, 12.dp, 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries, key = { it.fileName }) { entry ->
                        EntryCard(
                            entry,
                            onClick = { onOpen(entry.fileName) },
                            onDelete = { vm.delete(entry) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyDiary(modifier: Modifier) {
    Column(modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("У вас пока нет записей", fontSize = 22.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "Нажмите +, чтобы создать первую",
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EntryCard(entry: DiaryEntry, onClick: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box {
        Card(
            Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = { menu = true })
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    dateFormat.format(Date(entry.time)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (entry.title.isNotBlank()) {
                    Text(entry.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                // первые 40 символов текста
                val preview = entry.text.replace('\n', ' ')
                Text(if (preview.length > 40) preview.take(40) + "…" else preview)
            }
        }
        // контекстное меню по долгому нажатию
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text("Удалить") },
                onClick = {
                    menu = false
                    onDelete()
                }
            )
        }
    }
}

@Composable
fun DiaryEditScreen(
    entry: DiaryEntry?,
    onSave: (String, String) -> Unit,
    onBack: () -> Unit
) {
    var title by rememberSaveable { mutableStateOf(entry?.title.orEmpty()) }
    var text by rememberSaveable { mutableStateOf(entry?.text.orEmpty()) }
    Scaffold(
        topBar = {
            AppBar(if (entry == null) "Новая запись" else "Запись", onBack)
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .imePadding()
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Заголовок (необязательно)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Текст заметки") },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                    Text("Назад")
                }
                Button(
                    onClick = { onSave(title, text) },
                    enabled = text.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) { Text("Сохранить") }
            }
        }
    }
}

const val NEW = "new"
