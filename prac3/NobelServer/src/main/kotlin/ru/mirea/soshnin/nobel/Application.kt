package ru.mirea.soshnin.nobel

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import ru.mirea.soshnin.nobel.data.db.*
import ru.mirea.soshnin.nobel.data.memory.InMemoryFavoriteRepository
import ru.mirea.soshnin.nobel.data.memory.InMemoryPrizeRepository
import ru.mirea.soshnin.nobel.data.memory.InMemoryUserRepository
import ru.mirea.soshnin.nobel.domain.repository.FavoriteRepository
import ru.mirea.soshnin.nobel.domain.repository.PrizeRepository
import ru.mirea.soshnin.nobel.domain.repository.UserRepository
import ru.mirea.soshnin.nobel.domain.usecase.*
import ru.mirea.soshnin.nobel.plugins.*
import ru.mirea.soshnin.nobel.presentation.routes.authRoutes
import ru.mirea.soshnin.nobel.presentation.routes.prizeRoutes
import ru.mirea.soshnin.nobel.presentation.routes.userRoutes
import java.io.File
import java.net.URI

fun main() {
    embeddedServer(Netty, port = (System.getenv("PORT") ?: "8090").toInt(), host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

// Простой разбор .env, чтобы не хранить пароль от БД в коде
private fun env(name: String): String? = System.getenv(name) ?: File(".env")
    .takeIf { it.exists() }?.readLines()
    ?.firstOrNull { it.startsWith("$name=") }
    ?.substringAfter("=")?.trim()?.trim('"', '\'')

fun Application.module() {
    // DATABASE_URL задан - работаем с PostgreSQL (задание 5), иначе данные в памяти (задание 4)
    val databaseUrl = env("DATABASE_URL")
    val postgres = databaseUrl != null

    val prizes: PrizeRepository
    val users: UserRepository
    val favorites: FavoriteRepository
    if (postgres) {
        val uri = URI(databaseUrl!!.replace("postgresql://", "postgres://"))
        val (user, password) = uri.userInfo.split(":", limit = 2)
        val jdbc = "jdbc:postgresql://${uri.host}${uri.path}?sslmode=require"
        DatabaseFactory.init(jdbc, user, password)
        val prizeRepo = PostgresPrizeRepository()
        val userRepo = PostgresUserRepository()
        runBlocking {
            NobelImporter(prizeRepo).importIfEmpty()
            userRepo.createIfMissing("student", hashPassword("student123"), "user")
            userRepo.createIfMissing("admin", hashPassword("admin123"), "admin")
        }
        prizes = prizeRepo
        users = userRepo
        favorites = PostgresFavoriteRepository(prizeRepo)
        log.info("Хранилище: PostgreSQL (${uri.host})")
    } else {
        prizes = InMemoryPrizeRepository()
        users = InMemoryUserRepository()
        favorites = InMemoryFavoriteRepository(prizes)
        log.info("Хранилище: память (prizes.json)")
    }

    configureSerialization()
    configureMonitoring()
    configureStatusPages()
    configureSecurity()
    configureOpenApi()

    routing {
        authRoutes(LoginUseCase(users))
        val prizeRoutes: Route.() -> Unit = {
            prizeRoutes(GetPrizesUseCase(prizes), GetPrizeUseCase(prizes), GetLaureatesUseCase(prizes))
        }
        if (postgres) {
            // задание 5: список премий открытый
            prizeRoutes()
        }
        authenticate("auth-jwt") {
            // задание 4: все премии только с токеном
            if (!postgres) prizeRoutes()
            userRoutes(GetProfileUseCase(users), FavoritesUseCase(favorites, prizes))
        }
    }
}
