package at.ac.hcw

import at.ac.hcw.exceptions.CarAlreadyBookedException
import at.ac.hcw.exceptions.CarNotFoundException
import at.ac.hcw.exceptions.CarNotBookedException
import at.ac.hcw.exceptions.UserNotFoundException
import at.ac.hcw.model.Booking
import at.ac.hcw.routes.bookingRoutes
import at.ac.hcw.service.BookingService
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.mockk.coEvery
import io.mockk.mockk
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
import java.util.Date
import kotlin.test.*

class BookingRouteTest {

    private val secret = "car-rental-super-secret-key-change-me"
    private val issuer = "car-rental-service"
    private val audience = "car-rental-users"

    private val booking = Booking(id = "b1", userId = "u1", carId = "c1")
    private val bookingService = mockk<BookingService>()

    private fun userToken(userId: String): String = JWT.create()
        .withIssuer(issuer)
        .withAudience(audience)
        .withClaim("userId", userId)
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
                realm = "Car Rental Service"
                verifier(verifier)
                validate { credential ->
                    credential.payload.getClaim("userId").asString() ?: return@validate null
                    JWTPrincipal(credential.payload)
                }
                challenge { _, _ -> call.respond(HttpStatusCode.Unauthorized, "Authentication required") }
            }
            jwt("admin-jwt") {
                realm = "Car Rental Service Admin"
                verifier(verifier)
                validate { credential ->
                    val isAdmin = credential.payload.getClaim("isAdmin").asBoolean() ?: false
                    if (!isAdmin) return@validate null
                    JWTPrincipal(credential.payload)
                }
                challenge { _, _ -> call.respond(HttpStatusCode.Forbidden, "Admin privileges required") }
            }
        }
        install(ContentNegotiation) { json() }
        routing {
            route("/api") { bookingRoutes(bookingService) }
        }
    }

    // --- POST /api/bookings ---

    @Test
    fun `POST bookings - 201 when booking created`() = testApplication {
        setup()
        coEvery { bookingService.create(any()) } returns booking

        val response = client.post("/api/bookings") {
            header(HttpHeaders.Authorization, "Bearer ${userToken("u1")}")
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"u1","carId":"c1"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("\"id\":\"b1\""))
    }

    @Test
    fun `POST bookings - 401 when no token`() = testApplication {
        setup()

        val response = client.post("/api/bookings") {
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"u1","carId":"c1"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST bookings - 403 when token userId differs from request userId`() = testApplication {
        setup()

        val response = client.post("/api/bookings") {
            header(HttpHeaders.Authorization, "Bearer ${userToken("u2")}")
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"u1","carId":"c1"}""")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `POST bookings - 404 when user not found`() = testApplication {
        setup()
        coEvery { bookingService.create(any()) } throws UserNotFoundException("u1")

        val response = client.post("/api/bookings") {
            header(HttpHeaders.Authorization, "Bearer ${userToken("u1")}")
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"u1","carId":"c1"}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST bookings - 404 when car not found`() = testApplication {
        setup()
        coEvery { bookingService.create(any()) } throws CarNotFoundException("c1")

        val response = client.post("/api/bookings") {
            header(HttpHeaders.Authorization, "Bearer ${userToken("u1")}")
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"u1","carId":"c1"}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST bookings - 409 when car already booked`() = testApplication {
        setup()
        coEvery { bookingService.create(any()) } throws CarAlreadyBookedException("c1")

        val response = client.post("/api/bookings") {
            header(HttpHeaders.Authorization, "Bearer ${userToken("u1")}")
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"u1","carId":"c1"}""")
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    // --- DELETE /api/bookings/{id} ---

    @Test
    fun `DELETE bookings - 200 when owner cancels own booking`() = testApplication {
        setup()
        coEvery { bookingService.findById("b1") } returns booking
        coEvery { bookingService.cancel("b1") } returns booking

        val response = client.delete("/api/bookings/b1") {
            header(HttpHeaders.Authorization, "Bearer ${userToken("u1")}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `DELETE bookings - 200 when admin cancels another user's booking`() = testApplication {
        setup()
        coEvery { bookingService.findById("b1") } returns booking
        coEvery { bookingService.cancel("b1") } returns booking

        val response = client.delete("/api/bookings/b1") {
            header(HttpHeaders.Authorization, "Bearer ${adminToken()}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `DELETE bookings - 401 when no token`() = testApplication {
        setup()

        val response = client.delete("/api/bookings/b1")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `DELETE bookings - 403 when non-admin user cancels another user's booking`() = testApplication {
        setup()
        coEvery { bookingService.findById("b1") } returns booking

        val response = client.delete("/api/bookings/b1") {
            header(HttpHeaders.Authorization, "Bearer ${userToken("u2")}")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `DELETE bookings - 404 when booking not found`() = testApplication {
        setup()
        coEvery { bookingService.findById("unknown") } returns null

        val response = client.delete("/api/bookings/unknown") {
            header(HttpHeaders.Authorization, "Bearer ${userToken("u1")}")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `DELETE bookings - 404 when booking already cancelled`() = testApplication {
        setup()
        coEvery { bookingService.findById("b1") } returns booking
        coEvery { bookingService.cancel("b1") } throws CarNotBookedException("b1")

        val response = client.delete("/api/bookings/b1") {
            header(HttpHeaders.Authorization, "Bearer ${userToken("u1")}")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // --- GET /api/bookings?userId= ---

    @Test
    fun `GET bookings - 200 when user requests own bookings`() = testApplication {
        setup()
        coEvery { bookingService.findByUser("u1") } returns listOf(booking)

        val response = client.get("/api/bookings?userId=u1") {
            header(HttpHeaders.Authorization, "Bearer ${userToken("u1")}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"id\":\"b1\""))
    }

    @Test
    fun `GET bookings - 200 when admin requests another user's bookings`() = testApplication {
        setup()
        coEvery { bookingService.findByUser("u1") } returns listOf(booking)

        val response = client.get("/api/bookings?userId=u1") {
            header(HttpHeaders.Authorization, "Bearer ${adminToken()}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `GET bookings - 400 when userId query param missing`() = testApplication {
        setup()

        val response = client.get("/api/bookings") {
            header(HttpHeaders.Authorization, "Bearer ${userToken("u1")}")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET bookings - 401 when no token`() = testApplication {
        setup()

        val response = client.get("/api/bookings?userId=u1")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET bookings - 403 when user requests another user's bookings`() = testApplication {
        setup()

        val response = client.get("/api/bookings?userId=u1") {
            header(HttpHeaders.Authorization, "Bearer ${userToken("u2")}")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }
}
