package ru.mirea.soshnin.nobel.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import ru.mirea.soshnin.nobel.presentation.dto.MessageResponse
import java.util.Date

object JwtConfig {
    // ключ подписи не короче 32 символов
    val secret: String = System.getenv("JWT_SECRET")
        ?: "mirea-ikbo-65-23-soshnin-nobel-api-secret-key"
    const val ISSUER = "nobel-api"
    const val AUDIENCE = "nobel-client"
    const val LIFETIME_SEC = 30 * 60 // 30 минут

    private val algorithm = Algorithm.HMAC256(secret)

    fun makeToken(userId: Int, username: String): String = JWT.create()
        .withIssuer(ISSUER)
        .withAudience(AUDIENCE)
        .withClaim("userId", userId)
        .withClaim("username", username)
        .withExpiresAt(Date(System.currentTimeMillis() + LIFETIME_SEC * 1000L))
        .sign(algorithm)

    val verifier = JWT.require(algorithm)
        .withIssuer(ISSUER)
        .withAudience(AUDIENCE)
        .build()
}

fun Application.configureSecurity() {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "Nobel Prize API"
            verifier(JwtConfig.verifier)
            validate { cred ->
                if (cred.payload.getClaim("userId").asInt() != null) JWTPrincipal(cred.payload) else null
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    MessageResponse("Токен отсутствует или истек")
                )
            }
        }
    }
}

val ApplicationCall.userId: Int
    get() = principal<JWTPrincipal>()!!.payload.getClaim("userId").asInt()
