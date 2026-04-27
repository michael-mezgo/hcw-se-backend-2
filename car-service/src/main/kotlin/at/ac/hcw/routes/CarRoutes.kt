package at.ac.hcw.routes

import at.ac.hcw.dto.*
import at.ac.hcw.service.CarService
import currency.CurrencyServiceGrpcKt
import io.ktor.http.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Routing.carRoutes(
    carService: CarService,
    currencyClient: CurrencyServiceGrpcKt.CurrencyServiceCoroutineStub? = null,
    onCarCreated: suspend (CarEvent) -> Unit = {},
    onCarDeleted: suspend (CarEvent) -> Unit = {},
) {
    //TODO: Armin use SMILEY4 endpoints (DOCU)
    route("/cars") {
        get {
            val currency = call.request.queryParameters["currency"] ?: "USD"

            try {
                val cars = carService.getAllCars()
                    .map { it.toResponse(currencyClient, currency) }
                call.respond(HttpStatusCode.OK, cars)
            }
            catch (e: BadRequestException) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "")
            }

        }

        get("/{id}") {
            val id = call.parameters["id"]
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing car id")
                return@get
            }
            val currency = call.request.queryParameters["currency"] ?: "USD"
            val car = carService.getCar(id)
            if (car == null) {
                call.respond(HttpStatusCode.NotFound, "No Car with id $id found")
                return@get
            }

            try {
                val response = car.toResponse(currencyClient, currency)
                call.respond(HttpStatusCode.OK, response)
            } catch (e: BadRequestException) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
            }
        }

        post {
            val request = call.receive<CarCreateRequest>()
            val createdId = carService.createCar(request.toDomain())
            onCarCreated(CarEvent(createdId))
            call.respond(HttpStatusCode.Created, mapOf("id" to createdId))
        }

        patch("/{id}") {
            val id = call.parameters["id"]
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing car id")
                return@patch
            }
            val patchRequest = call.receive<CarPatchRequest>()
            val updatedCar = carService.patchCar(id, patchRequest)
            if (updatedCar == null) {
                call.respond(HttpStatusCode.NotFound, "No Car with id $id found")
                return@patch
            }

            try {
                val response = updatedCar.toResponse(currencyClient)
                call.respond(HttpStatusCode.OK, response)
            } catch (e: BadRequestException) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
            }
        }

        delete("/{id}") {
            val id = call.parameters["id"]
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing car id")
                return@delete
            }
            val deleted = carService.deleteCar(id)
            if (deleted) {
                onCarDeleted(CarEvent(id))
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound, "No Car with id $id found")
            }
        }
    }
}