package at.ac.hcw.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureSecurity() {

    val config = environment.config.config("jwt")

    val verifier = JWT.require(
        Algorithm.HMAC256(config.property("secret").getString())
    )
        .withIssuer(config.property("issuer").getString())
        .withAudience(config.property("audience").getString())
        .build()

    authentication {

        jwt("user-jwt") {
            this.verifier(verifier)

            validate { credential ->
                val userId = credential.payload.getClaim("userId").asString()
                if (userId != null) JWTPrincipal(credential.payload) else null
            }
        }

        jwt("admin-jwt") {
            this.verifier(verifier)

            validate { credential ->
                val userId = credential.payload.getClaim("userId").asString()
                val isAdmin = credential.payload.getClaim("isAdmin").asBoolean()

                if (userId != null && isAdmin == true) {
                    JWTPrincipal(credential.payload)
                } else null
            }
        }
    }
}