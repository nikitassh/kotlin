package ru.mirea.soshnin.nobel.presentation.routes

import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ru.mirea.soshnin.nobel.domain.model.Laureate
import ru.mirea.soshnin.nobel.domain.model.NobelPrize
import ru.mirea.soshnin.nobel.domain.usecase.*
import ru.mirea.soshnin.nobel.plugins.JwtConfig
import ru.mirea.soshnin.nobel.plugins.userId
import ru.mirea.soshnin.nobel.presentation.dto.*

fun Route.authRoutes(login: LoginUseCase) {
    // /auth/login - задание 4, /login - задание 5
    for (path in listOf("/auth/login", "/login")) {
        post(path, {
            tags = listOf("Авторизация")
            description = "Вход по логину и паролю, в ответе JWT на 30 минут"
            request { body<LoginRequest>() }
            response {
                code(HttpStatusCode.OK) { body<TokenResponse>() }
                code(HttpStatusCode.Unauthorized) { body<MessageResponse>() }
            }
        }) {
            val req = call.receive<LoginRequest>()
            val user = login(req.username, req.password)
            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, MessageResponse("Неверный логин или пароль"))
            } else {
                val token = JwtConfig.makeToken(user.id, user.username)
                call.respond(TokenResponse(token, user.username, JwtConfig.LIFETIME_SEC))
            }
        }
    }
}

fun Route.prizeRoutes(
    getPrizes: GetPrizesUseCase,
    getPrize: GetPrizeUseCase,
    getLaureates: GetLaureatesUseCase
) {
    get("/prizes", {
        tags = listOf("Премии")
        description = "Список премий, можно фильтровать по году и категории"
        request {
            queryParameter<Int>("year") { description = "Год, например 2023" }
            queryParameter<String>("category") { description = "phy, che, med, lit, pea, eco" }
        }
        response { code(HttpStatusCode.OK) { body<List<NobelPrize>>() } }
    }) {
        val year = call.request.queryParameters["year"]?.toIntOrNull()
        val category = call.request.queryParameters["category"]
        call.respond(getPrizes(year, category))
    }

    get("/prizes/{year}/{category}", {
        tags = listOf("Премии")
        description = "Одна премия по году и категории"
        request {
            pathParameter<Int>("year")
            pathParameter<String>("category")
        }
        response {
            code(HttpStatusCode.OK) { body<NobelPrize>() }
            code(HttpStatusCode.NotFound) { body<MessageResponse>() }
        }
    }) {
        val (year, category) = call.yearAndCategory()
        val prize = getPrize(year, category)
        if (prize == null) call.respond(HttpStatusCode.NotFound, MessageResponse("Премия не найдена"))
        else call.respond(prize)
    }

    get("/prizes/{year}/{category}/laureates", {
        tags = listOf("Премии")
        description = "Лауреаты премии"
        request {
            pathParameter<Int>("year")
            pathParameter<String>("category")
        }
        response { code(HttpStatusCode.OK) { body<List<Laureate>>() } }
    }) {
        val (year, category) = call.yearAndCategory()
        val list = getLaureates(year, category)
        if (list == null) call.respond(HttpStatusCode.NotFound, MessageResponse("Премия не найдена"))
        else call.respond(list)
    }
}

fun Route.userRoutes(profile: GetProfileUseCase, favorites: FavoritesUseCase) {
    get("/users/me", {
        tags = listOf("Пользователь")
        description = "Профиль текущего пользователя"
        response { code(HttpStatusCode.OK) { body<ProfileResponse>() } }
    }) {
        val user = profile(call.userId)!!
        call.respond(ProfileResponse(user.id, user.username, user.role))
    }

    get("/users/me/prizes", {
        tags = listOf("Пользователь")
        description = "Избранные премии"
        response { code(HttpStatusCode.OK) { body<List<NobelPrize>>() } }
    }) {
        call.respond(favorites.list(call.userId))
    }

    post("/users/me/prizes/{prizeId}", {
        tags = listOf("Пользователь")
        description = "Добавить премию в избранное"
        request { pathParameter<Int>("prizeId") }
        response {
            code(HttpStatusCode.Created) { body<MessageResponse>() }
            code(HttpStatusCode.NotFound) { body<MessageResponse>() }
        }
    }) {
        val id = call.prizeId()
        if (favorites.add(call.userId, id)) {
            call.respond(HttpStatusCode.Created, MessageResponse("Премия $id добавлена в избранное"))
        } else {
            call.respond(HttpStatusCode.NotFound, MessageResponse("Премия не найдена"))
        }
    }

    delete("/users/me/prizes/{prizeId}", {
        tags = listOf("Пользователь")
        description = "Удалить премию из избранного"
        request { pathParameter<Int>("prizeId") }
        response { code(HttpStatusCode.OK) { body<MessageResponse>() } }
    }) {
        val id = call.prizeId()
        favorites.remove(call.userId, id)
        call.respond(MessageResponse("Премия $id удалена из избранного"))
    }
}

private fun io.ktor.server.application.ApplicationCall.yearAndCategory(): Pair<Int, String> {
    val year = parameters["year"]?.toIntOrNull() ?: throw BadRequestException("Год должен быть числом")
    return year to parameters["category"]!!
}

private fun io.ktor.server.application.ApplicationCall.prizeId(): Int =
    parameters["prizeId"]?.toIntOrNull() ?: throw BadRequestException("prizeId должен быть числом")
