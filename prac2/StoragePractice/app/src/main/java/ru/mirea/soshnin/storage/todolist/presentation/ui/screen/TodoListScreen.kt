package ru.mirea.soshnin.storage.todolist.presentation.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.mirea.soshnin.storage.todolist.presentation.ui.component.TodoItemCard
import ru.mirea.soshnin.storage.todolist.presentation.viewmodel.TodoViewModel
import ru.mirea.soshnin.storage.ui.AppBar

@Composable
fun TodoListScreen(
    vm: TodoViewModel,
    onExit: () -> Unit,
    onEdit: (Int) -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            AppBar("TodoList", onExit) {
                // переключатель в TopAppBar, значение в DataStore
                Text("Цвет завершенных", fontSize = 13.sp, color = Color.White)
                Switch(
                    checked = state.completedColor,
                    onCheckedChange = vm::setCompletedColor,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF66BB6A))
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onEdit(0) }) { Icon(Icons.Default.Add, "Добавить") }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.todos.isEmpty() -> Text("Задач нет", Modifier.align(Alignment.Center))
                else -> LazyColumn(
                    contentPadding = PaddingValues(12.dp, 12.dp, 12.dp, 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.todos, key = { it.id }) { todo ->
                        TodoItemCard(
                            todo = todo,
                            highlight = state.completedColor,
                            onToggle = { vm.toggle(todo) },
                            onClick = { onEdit(todo.id) },
                            onDelete = { vm.delete(todo) }
                        )
                    }
                }
            }
        }
    }
}
