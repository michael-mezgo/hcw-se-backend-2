package at.ac.hcw.security

import at.ac.hcw.database.DatabaseUser
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

private const val JWT_EXPIRY_MS = 86_400_000L

fun generateToken(user: DatabaseUser): String =
    JWT.create()
        .withIssuer(JwtConfig.issuer)
        .withAudience(JwtConfig.audience)
        .withClaim("userId", user.id.toHexString())
        .withClaim("username", user.username)
        .withClaim("isAdmin", user.isAdmin)
        .withExpiresAt(Date(System.currentTimeMillis() + JWT_EXPIRY_MS))
        .sign(Algorithm.HMAC256(JwtConfig.secret))