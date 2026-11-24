package ru.mirea.soshnin.storage.todolist.data.model

import kotlinx.serialization.Serializable

// формат файла assets/todos.json
@Serializable
data class TodoJson(
    val id: Int,
    val title: String,
    val description: String,
    val isCompleted: Boolean
)
