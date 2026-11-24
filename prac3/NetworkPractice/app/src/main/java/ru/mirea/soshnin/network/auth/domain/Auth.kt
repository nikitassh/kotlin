package ru.mirea.soshnin.network.auth.domain

data class User(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val username: String,
    val email: String,
    val image: String,
    val phone: String = "",
    val age: Int = 0
) {
    val fullName get() = "$firstName $lastName"
}

class WrongCredentialsException : Exception("Неверные данные")

interface AuthRepository {
    suspend fun hasToken(): Boolean
    suspend fun login(username: String, password: String): User
    suspend fun logout()
    suspend fun getUsers(): List<User>
    suspend fun getUser(id: Int): User
}

class LoginUseCase(private val repo: AuthRepository) {
    suspend operator fun invoke(username: String, password: String): User {
        require(username.isNotBlank() && password.isNotBlank()) { "Заполните логин и пароль" }
        return repo.login(username.trim(), password)
    }
}

class GetUsersUseCase(private val repo: AuthRepository) {
    suspend operator fun invoke() = repo.getUsers()
}

class GetUserUseCase(private val repo: AuthRepository) {
    suspend operator fun invoke(id: Int) = repo.getUser(id)
}

class LogoutUseCase(private val repo: AuthRepository) {
    suspend operator fun invoke() = repo.logout()
}
