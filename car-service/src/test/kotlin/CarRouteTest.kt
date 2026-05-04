package at.ac.hcw

import at.ac.hcw.domain.Car
import at.ac.hcw.domain.FuelType
import at.ac.hcw.domain.Transmission
import at.ac.hcw.dto.Location
import at.ac.hcw.routes.carRoutes
import at.ac.hcw.service.CarService
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CarRouteTest {

    private val secret = "car-rental-super-secret-key-change-me"
    private val issuer = "car-rental-service"
    private val audience = "car-rental-users"

    private val carService = mockk<CarService>()

    private val car = Car(
        id = "c1",
        manufacturer = "Toyota",
        model = "Corolla",
        year = 2022,
        pricePerDay = 50.0,
        description = "A reliable car",
        imageName = "cars/c1.jpg",
        transmission = Transmission.AUTOMATIC,
        power = 132,
        fuelType = FuelType.GASOLINE,
        available = true,
        location = Location(48.2, 16.4)
    )

    private fun adminToken(userId: String = "admin"): String = JWT.create()
        .withIssuer(issuer)
        .withAudience(audience)
        .withClaim("userId", userId)
        .withClaim("isAdmin", true)
        .withExpiresAt(Date(System.currentTimeMillis() + 60_000))
        .sign(Algorithm.HMAC256(secret))

    private fun userToken(userId: String = "u1"): String = JWT.create()
        .withIssuer(issuer)
        .withAudience(audience)
        .withClaim("userId", userId)
        .withClaim("isAdmin", false)
        .withExpiresAt(Date(System.currentTimeMillis() + 60_000))
        .sign(Algorithm.HMAC256(secret))

    private fun ApplicationTestBuilder.setup() {
        val algorithm = Algorithm.HMAC256(secret)
        val verifier = JWT.require(algorithm).withIssuer(issuer).withAudience(audience).build()

        install(Authentication) {
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
            route("/api") { carRoutes(carService) }
        }
    }

    // --- GET /api/cars ---

    @Test
    fun `GET cars - 200 returns all cars`() = testApplication {
        setup()
        coEvery { carService.getAllCars() } returns listOf(car)

        val response = client.get("/api/cars")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"id\":\"c1\""))
    }

    @Test
    fun `GET cars - 200 returns only available cars when filter set`() = testApplication {
        setup()
        coEvery { carService.getAllAvailableCars() } returns listOf(car)

        val response = client.get("/api/cars?available=true")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"id\":\"c1\""))
    }

    @Test
    fun `GET cars - 200 returns empty list when no cars`() = testApplication {
        setup()
        coEvery { carService.getAllCars() } returns emptyList()

        val response = client.get("/api/cars")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText())
    }

    @Test
    fun `GET cars - 500 when service throws`() = testApplication {
        setup()
        coEvery { carService.getAllCars() } throws RuntimeException("boom")

        val response = client.get("/api/cars")

        assertEquals(HttpStatusCode.InternalServerError, response.status)
    }

    // --- GET /api/cars/{id} ---

    @Test
    fun `GET car by id - 200 when found`() = testApplication {
        setup()
        coEvery { carService.getCar("c1") } returns car

        val response = client.get("/api/cars/c1")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"id\":\"c1\""))
    }

    @Test
    fun `GET car by id - 404 when not found`() = testApplication {
        setup()
        coEvery { carService.getCar("unknown") } returns null

        val response = client.get("/api/cars/unknown")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // --- DELETE /api/cars/{id} ---

    @Test
    fun `DELETE car - 204 when admin deletes existing car`() = testApplication {
        setup()
        coEvery { carService.deleteCar("c1") } returns true

        val response = client.delete("/api/cars/c1") {
            header(HttpHeaders.Authorization, "Bearer ${adminToken()}")
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `DELETE car - 404 when car not found`() = testApplication {
        setup()
        coEvery { carService.deleteCar("unknown") } returns false

        val response = client.delete("/api/cars/unknown") {
            header(HttpHeaders.Authorization, "Bearer ${adminToken()}")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `DELETE car - 403 when no token`() = testApplication {
        setup()

        val response = client.delete("/api/cars/c1")

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `DELETE car - 403 when non-admin token`() = testApplication {
        setup()

        val response = client.delete("/api/cars/c1") {
            header(HttpHeaders.Authorization, "Bearer ${userToken()}")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }
}
