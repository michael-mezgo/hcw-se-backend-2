package at.ac.hcw.security

import at.ac.hcw.dto.JwtPrincipal
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*

fun Application.configureSecurity() {

    val algorithm = Algorithm.HMAC256(JWT_SECRET)

    val verifier = JWT.require(algorithm)
        .withIssuer(JWT_ISSUER)
        .withAudience(JWT_AUDIENCE)
        .build()

    install(Authentication) {

        jwt("user-jwt") {
            realm = "App"
            this.verifier(verifier)

            validate { credential ->
                val userId = credential.payload.getClaim("userId").asString()
                    ?: return@validate null

                val username = credential.payload.getClaim("username").asString()
                    ?: return@validate null

                val isAdmin = credential.payload.getClaim("isAdmin").asBoolean() ?: false

                JwtPrincipal(userId, username, isAdmin)
            }

            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("error" to "Authentication required")
                )
            }
        }

        jwt("admin-jwt") {
            realm = "App Admin"
            this.verifier(verifier)

            validate { credential ->
                val userId = credential.payload.getClaim("userId").asString()
                    ?: return@validate null

                val username = credential.payload.getClaim("username").asString()
                    ?: return@validate null

                val isAdmin = credential.payload.getClaim("isAdmin").asBoolean() ?: false

                if (!isAdmin) return@validate null

                JwtPrincipal(userId, username, true)
            }

            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf("error" to "Admin privileges required")
                )
            }
        }
    }
}