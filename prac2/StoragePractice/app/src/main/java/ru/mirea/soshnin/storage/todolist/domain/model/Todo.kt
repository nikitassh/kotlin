package ru.mirea.soshnin.storage.todolist.domain.model

data class Todo(
    val id: Int = 0,
    val title: String,
    val description: String,
    val isCompleted: Boolean = false
)
