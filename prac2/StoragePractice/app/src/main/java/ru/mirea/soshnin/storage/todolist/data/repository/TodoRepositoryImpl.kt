package ru.mirea.soshnin.storage.todolist.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import ru.mirea.soshnin.storage.todolist.data.local.TodoDao
import ru.mirea.soshnin.storage.todolist.data.local.TodoEntity
import ru.mirea.soshnin.storage.todolist.data.model.TodoJson
import ru.mirea.soshnin.storage.todolist.data.preferences.SettingsDataStore
import ru.mirea.soshnin.storage.todolist.domain.model.Todo
import ru.mirea.soshnin.storage.todolist.domain.repository.TodoRepository

class TodoRepositoryImpl(
    private val context: Context,
    private val dao: TodoDao,
    private val settings: SettingsDataStore
) : TodoRepository {

    override fun observeTodos(): Flow<List<Todo>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getTodo(id: Int) = dao.getById(id)?.toDomain()

    override suspend fun add(todo: Todo) = dao.insert(todo.toEntity())

    override suspend fun update(todo: Todo) = dao.update(todo.toEntity())

    override suspend fun delete(todo: Todo) = dao.delete(todo.toEntity())

    // Разовый импорт: флаг в DataStore + проверка, что таблица пустая
    override suspend fun importFromJsonOnce() = withContext(Dispatchers.IO) {
        if (settings.isImported() || dao.count() > 0) return@withContext
        val text = context.assets.open("todos.json").bufferedReader().use { it.readText() }
        val items = Json.decodeFromString<List<TodoJson>>(text)
        dao.insertAll(items.map {
            TodoEntity(title = it.title, description = it.description, isCompleted = it.isCompleted)
        })
        settings.setImported()
    }
}

private fun TodoEntity.toDomain() = Todo(id, title, description, isCompleted)
private fun Todo.toEntity() = TodoEntity(id, title, description, isCompleted)
