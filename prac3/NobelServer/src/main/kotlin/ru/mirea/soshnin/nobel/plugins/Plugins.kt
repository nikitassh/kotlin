package ru.mirea.soshnin.nobel.plugins

import io.github.smiley4.ktoropenapi.OpenApi
import io.github.smiley4.ktoropenapi.config.AuthScheme
import io.github.smiley4.ktoropenapi.config.AuthType
import io.github.smiley4.ktoropenapi.openApi
import io.github.smiley4.ktorredoc.redoc
import io.github.smiley4.ktorswaggerui.swaggerUI
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.slf4j.event.Level
import ru.mirea.soshnin.nobel.presentation.dto.MessageResponse

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json { prettyPrint = true; explicitNulls = false })
    }
}

// Логирование всех запросов в консоль
fun Application.configureMonitoring() {
    install(CallLogging) {
        level = Level.INFO
        format { call ->
            val status = call.response.status()?.value
            val auth = if (call.request.headers["Authorization"] != null) "JWT" else "-"
            "${call.request.httpMethod.value} ${call.request.uri} -> $status [$auth]"
        }
    }
}

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<BadRequestException> { call, e ->
            call.respond(HttpStatusCode.BadRequest, MessageResponse(e.message ?: "Неверный запрос"))
        }
        exception<Throwable> { call, e ->
            call.application.log.error("Ошибка", e)
            call.respond(HttpStatusCode.InternalServerError, MessageResponse("Внутренняя ошибка сервера"))
        }
    }
}

// Описание API: /swagger и /redoc
fun Application.configureOpenApi() {
    install(OpenApi) {
        info {
            title = "Nobel Prize API"
            version = "1.0"
            description = "Практика РТУ МИРЭА, Сошнин Н.А., ИКБО-65-23"
        }
        security {
            securityScheme("jwt") {
                type = AuthType.HTTP
                scheme = AuthScheme.BEARER
                bearerFormat = "JWT"
            }
            defaultSecuritySchemeNames("jwt")
        }
    }
    routing {
        route("api.json") { openApi() }
        route("swagger") { swaggerUI("/api.json") }
        route("redoc") { redoc("/api.json") }
    }
}
