package at.ac.hcw

import io.ktor.server.application.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.config.tryGetString
import io.ktor.server.response.*

fun Application.configureSecurity() {
    val jwtSecret: String = environment.config.tryGetString("jwt.secret") ?: "car-rental-super-secret-key-change-me"
    val jwtIssuer = environment.config.tryGetString("jwt.issuer") ?: "car-rental-service"
    val jwtAudience = environment.config.tryGetString("jwt.audience") ?: "car-rental-users"

    val algorithm = Algorithm.HMAC256(jwtSecret)
    val verifier = JWT.require(algorithm).withIssuer(jwtIssuer).withAudience(jwtAudience).build()

    authentication {
        jwt("user-jwt") {
            realm = "Car Rental Service"
            this.verifier(verifier)
            validate { credential ->
                credential.payload.getClaim("userId").asString() ?: return@validate null
                JWTPrincipal(credential.payload)
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Authentication required"))
            }
        }
        jwt("admin-jwt") {
            realm = "Car Rental Service Admin"
            this.verifier(verifier)
            validate { credential ->
                val isAdmin = credential.payload.getClaim("isAdmin").asBoolean() ?: false
                if (!isAdmin) return@validate null
                JWTPrincipal(credential.payload)
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin privileges required"))
            }
        }
    }
}