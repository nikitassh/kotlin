package ru.mirea.soshnin.storage.todolist.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.mirea.soshnin.storage.todolist.domain.model.Todo

interface TodoRepository {
    fun observeTodos(): Flow<List<Todo>>
    suspend fun getTodo(id: Int): Todo?
    suspend fun add(todo: Todo)
    suspend fun update(todo: Todo)
    suspend fun delete(todo: Todo)
    suspend fun importFromJsonOnce()
}
