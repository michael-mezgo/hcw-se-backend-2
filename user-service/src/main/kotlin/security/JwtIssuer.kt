package at.ac.hcw.security

import at.ac.hcw.database.DatabaseUser
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

val JWT_SECRET: String = System.getenv("JWT_SECRET")
    ?: error("JWT_SECRET environment variable is not set")
const val JWT_ISSUER = "car-rental-service"
const val JWT_AUDIENCE = "car-rental-users"
private const val JWT_EXPIRY_MS = 86_400_000L // 24h

fun generateToken(user: DatabaseUser): String =
    JWT.create()
        .withIssuer(JWT_ISSUER)
        .withAudience(JWT_AUDIENCE)
        .withClaim("userId", user.id.toHexString())
        .withClaim("username", user.username)
        .withClaim("isAdmin", user.isAdmin)
        .withExpiresAt(Date(System.currentTimeMillis() + JWT_EXPIRY_MS))
        .sign(Algorithm.HMAC256(JWT_SECRET))