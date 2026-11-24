package ru.mirea.soshnin.network.auth.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.api.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.mirea.soshnin.network.auth.domain.AuthRepository
import ru.mirea.soshnin.network.auth.domain.User
import ru.mirea.soshnin.network.auth.domain.WrongCredentialsException

private val Context.authStore by preferencesDataStore("auth")
private val TOKEN = stringPreferencesKey("token")

// Хранение токена в Preferences DataStore
class TokenStore(private val context: Context) {
    suspend fun get(): String? = context.authStore.data.map { it[TOKEN] }.first()
    suspend fun save(token: String) = context.authStore.edit { it[TOKEN] = token }
    suspend fun clear() = context.authStore.edit { it.remove(TOKEN) }
}

@Serializable
data class LoginRequest(val username: String, val password: String, val expiresInMins: Int = 60)

@Serializable
data class LoginResponse(
    val id: Int, val username: String, val email: String,
    val firstName: String, val lastName: String, val image: String,
    val accessToken: String
)

@Serializable
data class UserDto(
    val id: Int, val firstName: String, val lastName: String,
    val username: String, val email: String, val image: String,
    val phone: String = "", val age: Int = 0
)

@Serializable
data class UsersResponse(val users: List<UserDto>, val total: Int)

private fun UserDto.toDomain() = User(id, firstName, lastName, username, email, image, phone, age)

// Плагин: Authorization: Bearer <token> ко всем запросам после входа
private fun bearerPlugin(store: TokenStore) = createClientPlugin("BearerFromStore") {
    onRequest { request, _ ->
        store.get()?.let { request.headers[HttpHeaders.Authorization] = "Bearer $it" }
    }
}

class AuthRepositoryImpl(context: Context) : AuthRepository {
    private val store = TokenStore(context)
    private val client = HttpClient(OkHttp) {
        expectSuccess = true
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(Logging) {
            logger = ru.mirea.soshnin.network.ui.AndroidHttpLogger
            level = LogLevel.HEADERS
        }
        install(HttpTimeout) { requestTimeoutMillis = 15_000 }
        install(bearerPlugin(store))
        defaultRequest { url("https://dummyjson.com/") }
    }

    override suspend fun hasToken() = store.get() != null

    override suspend fun login(username: String, password: String): User {
        val response: LoginResponse = try {
            client.post("auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(username, password))
            }.body()
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.BadRequest ||
                e.response.status == HttpStatusCode.Unauthorized
            ) throw WrongCredentialsException()
            throw e
        }
        store.save(response.accessToken)
        return User(
            response.id, response.firstName, response.lastName,
            response.username, response.email, response.image
        )
    }

    override suspend fun logout() {
        store.clear()
    }

    override suspend fun getUsers(): List<User> =
        client.get("users") { parameter("limit", 30) }.body<UsersResponse>().users.map { it.toDomain() }

    override suspend fun getUser(id: Int): User = client.get("users/$id").body<UserDto>().toDomain()
}
