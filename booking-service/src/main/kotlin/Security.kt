package at.ac.hcw

import io.ktor.server.application.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable

val JWT_SECRET: String = System.getenv("JWT_SECRET") ?: "car-rental-super-secret-key-change-me"
const val JWT_ISSUER = "car-rental-service"
const val JWT_AUDIENCE = "car-rental-users"

fun Application.configureSecurity() {
    val algorithm = Algorithm.HMAC256(JWT_SECRET)
    val verifier = JWT.require(algorithm).withIssuer(JWT_ISSUER).withAudience(JWT_AUDIENCE).build()

    authentication {
        jwt("user-jwt") {
            realm = "Car Rental Service"
            this.verifier(verifier)
            validate { credential ->
                val userId = credential.payload.getClaim("userId").asString() ?: return@validate null
                val username = credential.payload.getClaim("username").asString() ?: return@validate null
                val isAdmin = credential.payload.getClaim("isAdmin").asBoolean() ?: false
                JwtPrincipal(userId = userId, username = username, isAdmin = isAdmin)
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Authentication required"))
            }
        }
        jwt("admin-jwt") {
            realm = "Car Rental Service Admin"
            this.verifier(verifier)
            validate { credential ->
                val userId = credential.payload.getClaim("userId").asString() ?: return@validate null
                val username = credential.payload.getClaim("username").asString() ?: return@validate null
                val isAdmin = credential.payload.getClaim("isAdmin").asBoolean() ?: false
                if (!isAdmin) return@validate null
                JwtPrincipal(userId = userId, username = username, isAdmin = true)
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin privileges required"))
            }
        }
    }
}

/** JWT claims of the authenticated user. */
@Serializable
data class JwtPrincipal(val userId: String, val username: String, val isAdmin: Boolean = false)