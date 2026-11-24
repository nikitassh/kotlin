package ru.mirea.soshnin.storage.todolist.presentation.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.mirea.soshnin.storage.todolist.domain.model.Todo
import ru.mirea.soshnin.storage.todolist.presentation.viewmodel.TodoViewModel
import ru.mirea.soshnin.storage.ui.AppBar

@Composable
fun TodoEditScreen(vm: TodoViewModel, id: Int, onBack: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var completed by remember { mutableStateOf(false) }
    LaunchedEffect(id) {
        if (id != 0) {
            vm.getTodo(id)?.let {
                title = it.title
                description = it.description
                completed = it.isCompleted
            }
        }
    }
    Scaffold(
        topBar = { AppBar(if (id == 0) "Новая задача" else "Изменение задачи", onBack) }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Название") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = description, onValueChange = { description = it },
                label = { Text("Описание") }, minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = completed, onCheckedChange = { completed = it })
                Text("Выполнена")
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    vm.save(Todo(id, title.trim(), description.trim(), completed))
                    onBack()
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Сохранить") }
        }
    }
}
