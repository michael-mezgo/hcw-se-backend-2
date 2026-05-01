package at.ac.hcw.security

import io.ktor.server.application.*

object JwtConfig {
    lateinit var secret: String
    lateinit var issuer: String
    lateinit var audience: String
}

fun Application.loadJwtConfig() {
    val config = environment.config.config("jwt")

    JwtConfig.secret = config.property("secret").getString()
    JwtConfig.issuer = config.property("issuer").getString()
    JwtConfig.audience = config.property("audience").getString()
}