package ru.mirea.soshnin.storage.todolist.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.mirea.soshnin.storage.todolist.data.local.TodoDatabase
import ru.mirea.soshnin.storage.todolist.data.preferences.SettingsDataStore
import ru.mirea.soshnin.storage.todolist.data.repository.TodoRepositoryImpl
import ru.mirea.soshnin.storage.todolist.domain.model.Todo
import ru.mirea.soshnin.storage.todolist.domain.usecase.*

data class TodoUiState(
    val todos: List<Todo> = emptyList(),
    val completedColor: Boolean = true,
    val loading: Boolean = true
)

// Зависимости собираются вручную, без DI-фреймворка
class TodoViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = SettingsDataStore(app)
    private val repo = TodoRepositoryImpl(app, TodoDatabase.get(app).todoDao(), settings)

    private val getTodos = GetTodosUseCase(repo)
    val getTodo = GetTodoUseCase(repo)
    private val saveTodo = SaveTodoUseCase(repo)
    private val toggleTodo = ToggleTodoUseCase(repo)
    private val deleteTodo = DeleteTodoUseCase(repo)
    private val importTodos = ImportTodosUseCase(repo)

    val state: StateFlow<TodoUiState> =
        combine(getTodos(), settings.completedColor) { todos, color ->
            TodoUiState(todos, color, loading = false)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodoUiState())

    init {
        viewModelScope.launch { importTodos() }
    }

    fun save(todo: Todo) = viewModelScope.launch { saveTodo(todo) }
    fun toggle(todo: Todo) = viewModelScope.launch { toggleTodo(todo) }
    fun delete(todo: Todo) = viewModelScope.launch { deleteTodo(todo) }
    fun setCompletedColor(on: Boolean) = viewModelScope.launch { settings.setCompletedColor(on) }
}
