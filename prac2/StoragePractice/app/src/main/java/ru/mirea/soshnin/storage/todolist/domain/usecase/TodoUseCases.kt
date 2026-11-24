package ru.mirea.soshnin.storage.todolist.domain.usecase

import ru.mirea.soshnin.storage.todolist.domain.model.Todo
import ru.mirea.soshnin.storage.todolist.domain.repository.TodoRepository

class GetTodosUseCase(private val repo: TodoRepository) {
    operator fun invoke() = repo.observeTodos()
}

class GetTodoUseCase(private val repo: TodoRepository) {
    suspend operator fun invoke(id: Int) = repo.getTodo(id)
}

class SaveTodoUseCase(private val repo: TodoRepository) {
    suspend operator fun invoke(todo: Todo) {
        require(todo.title.isNotBlank()) { "Пустой заголовок" }
        if (todo.id == 0) repo.add(todo) else repo.update(todo)
    }
}

class ToggleTodoUseCase(private val repo: TodoRepository) {
    suspend operator fun invoke(todo: Todo) =
        repo.update(todo.copy(isCompleted = !todo.isCompleted))
}

class DeleteTodoUseCase(private val repo: TodoRepository) {
    suspend operator fun invoke(todo: Todo) = repo.delete(todo)
}

class ImportTodosUseCase(private val repo: TodoRepository) {
    suspend operator fun invoke() = repo.importFromJsonOnce()
}
