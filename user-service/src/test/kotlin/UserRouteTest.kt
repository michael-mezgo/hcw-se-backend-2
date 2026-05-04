package at.ac.hcw

import at.ac.hcw.database.DatabaseUser
import at.ac.hcw.dto.LoginResponse
import at.ac.hcw.exceptions.UnauthorizedException
import at.ac.hcw.exceptions.UserExistsException
import at.ac.hcw.routes.adminRoutes
import at.ac.hcw.routes.userRoutes
import at.ac.hcw.security.JwtConfig
import at.ac.hcw.service.AuthService
import at.ac.hcw.service.UserService
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import java.util.Date
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserRouteTest {

    private val secret = "test-secret"
    private val issuer = "test-issuer"
    private val audience = "test-audience"

    private val userService = mockk<UserService>()
    private val authService = mockk<AuthService>()

    private val user = DatabaseUser(
        id = "u1",
        username = "alice",
        email = "alice@example.com",
        passwordHash = "hashed",
        firstName = "Alice",
        lastName = "Adams",
        licenseNumber = "L-1",
        licenseValidUntil = "2030-12-31",
        isAdmin = false,
        preferredCurrency = "USD"
    )

    @BeforeTest
    fun setupJwtConfig() {
        JwtConfig.secret = secret
        JwtConfig.issuer = issuer
        JwtConfig.audience = audience
    }

    private fun userToken(userId: String = "u1"): String = JWT.create()
        .withIssuer(issuer)
        .withAudience(audience)
        .withClaim("userId", userId)
        .withClaim("isAdmin", false)
        .withExpiresAt(Date(System.currentTimeMillis() + 60_000))
        .sign(Algorithm.HMAC256(secret))

    private fun adminToken(userId: String = "admin"): String = JWT.create()
        .withIssuer(issuer)
        .withAudience(audience)
        .withClaim("userId", userId)
        .withClaim("isAdmin", true)
        .withExpiresAt(Date(System.currentTimeMillis() + 60_000))
        .sign(Algorithm.HMAC256(secret))

    private fun ApplicationTestBuilder.setup() {
        val algorithm = Algorithm.HMAC256(secret)
        val verifier = JWT.require(algorithm).withIssuer(issuer).withAudience(audience).build()

        install(Authentication) {
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
                    if (userId != null && isAdmin == true) JWTPrincipal(credential.payload) else null
                }
            }
        }
        install(ContentNegotiation) { json() }
        routing {
            userRoutes(userService, authService)
            adminRoutes(userService)
        }
    }

    // --- POST /auth/register ---

    @Test
    fun `POST register - 201 when user created`() = testApplication {
        setup()
        coEvery { authService.register(any()) } returns user

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(
                """{"username":"alice","email":"alice@example.com","password":"secret",
                "firstName":"Alice","lastName":"Adams","licenseNumber":"L-1",
                "preferredCurrency":"USD","licenseValidUntil":"2030-12-31"}""".trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("\"id\":\"u1\""))
    }

    @Test
    fun `POST register - 409 when user exists`() = testApplication {
        setup()
        coEvery { authService.register(any()) } throws UserExistsException("alice")

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(
                """{"username":"alice","email":"alice@example.com","password":"secret",
                "firstName":"Alice","lastName":"Adams","licenseNumber":"L-1",
                "preferredCurrency":"USD","licenseValidUntil":"2030-12-31"}""".trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    // --- POST /auth/login ---

    @Test
    fun `POST login - 200 returns token`() = testApplication {
        setup()
        coEvery { authService.login(any()) } returns LoginResponse(
            userId = "u1", isAdmin = false, token = "jwt-token"
        )

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"alice","password":"secret"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"token\":\"jwt-token\""))
    }

    @Test
    fun `POST login - 500 when credentials invalid`() = testApplication {
        setup()
        coEvery { authService.login(any()) } throws UnauthorizedException("alice")

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"alice","password":"wrong"}""")
        }

        assertEquals(HttpStatusCode.InternalServerError, response.status)
    }

    // --- GET /users/me ---

    @Test
    fun `GET users me - 200 returns own profile`() = testApplication {
        setup()
        coEvery { userService.findById("u1") } returns user

        val response = client.get("/users/me") {
            header(HttpHeaders.Authorization, "Bearer ${userToken("u1")}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"username\":\"alice\""))
    }

    @Test
    fun `GET users me - 404 when user not found`() = testApplication {
        setup()
        coEvery { userService.findById("u1") } returns null

        val response = client.get("/users/me") {
            header(HttpHeaders.Authorization, "Bearer ${userToken("u1")}")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET users me - 401 when no token`() = testApplication {
        setup()

        val response = client.get("/users/me")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    // --- PATCH /users/me ---

    @Test
    fun `PATCH users me - 200 when update succeeds`() = testApplication {
        setup()
        coEvery { userService.update(eq("u1"), any()) } returns user.copy(firstName = "Bob")

        val response = client.patch("/users/me") {
            header(HttpHeaders.Authorization, "Bearer ${userToken("u1")}")
            contentType(ContentType.Application.Json)
            setBody("""{"firstName":"Bob"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"firstName\":\"Bob\""))
    }

    @Test
    fun `PATCH users me - 404 when user not found`() = testApplication {
        setup()
        coEvery { userService.update(eq("u1"), any()) } returns null

        val response = client.patch("/users/me") {
            header(HttpHeaders.Authorization, "Bearer ${userToken("u1")}")
            contentType(ContentType.Application.Json)
            setBody("""{"firstName":"Bob"}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // --- DELETE /users/me ---

    @Test
    fun `DELETE users me - 204 when delete succeeds`() = testApplication {
        setup()
        coEvery { userService.delete("u1") } returns user

        val response = client.delete("/users/me") {
            header(HttpHeaders.Authorization, "Bearer ${userToken("u1")}")
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `DELETE users me - 404 when user not found`() = testApplication {
        setup()
        coEvery { userService.delete("u1") } returns null

        val response = client.delete("/users/me") {
            header(HttpHeaders.Authorization, "Bearer ${userToken("u1")}")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // --- ADMIN: GET /users ---

    @Test
    fun `GET admin users - 200 returns all users for admin`() = testApplication {
        setup()
        coEvery { userService.findAll() } returns listOf(user)

        val response = client.get("/users") {
            header(HttpHeaders.Authorization, "Bearer ${adminToken()}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"username\":\"alice\""))
    }

    @Test
    fun `GET admin users - 401 when no token`() = testApplication {
        setup()

        val response = client.get("/users")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET admin users - 401 when non-admin token`() = testApplication {
        setup()

        val response = client.get("/users") {
            header(HttpHeaders.Authorization, "Bearer ${userToken()}")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    // --- ADMIN: GET /users/{id} ---

    @Test
    fun `GET admin user by id - 200 when found`() = testApplication {
        setup()
        coEvery { userService.findById("u1") } returns user

        val response = client.get("/users/u1") {
            header(HttpHeaders.Authorization, "Bearer ${adminToken()}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"id\":\"u1\""))
    }

    @Test
    fun `GET admin user by id - 404 when not found`() = testApplication {
        setup()
        coEvery { userService.findById("unknown") } returns null

        val response = client.get("/users/unknown") {
            header(HttpHeaders.Authorization, "Bearer ${adminToken()}")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // --- ADMIN: DELETE /users/{id} ---

    @Test
    fun `DELETE admin user - 204 when delete succeeds`() = testApplication {
        setup()
        coEvery { userService.delete("u1") } returns user

        val response = client.delete("/users/u1") {
            header(HttpHeaders.Authorization, "Bearer ${adminToken("admin-id")}")
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `DELETE admin user - 403 when admin tries to delete themselves`() = testApplication {
        setup()

        val response = client.delete("/users/admin-id") {
            header(HttpHeaders.Authorization, "Bearer ${adminToken("admin-id")}")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `DELETE admin user - 404 when user not found`() = testApplication {
        setup()
        coEvery { userService.delete("unknown") } returns null

        val response = client.delete("/users/unknown") {
            header(HttpHeaders.Authorization, "Bearer ${adminToken("admin-id")}")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
