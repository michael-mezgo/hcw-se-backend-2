package at.ac.hcw.routes

import at.ac.hcw.dto.CarCreateRequest
import at.ac.hcw.dto.CarEvent
import at.ac.hcw.dto.CarPatchRequest
import at.ac.hcw.dto.toDomain
import at.ac.hcw.dto.toResponse
import at.ac.hcw.service.CarService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Routing.carRoutes(
    carService: CarService,
    onCarCreated: suspend (CarEvent) -> Unit = {},
    onCarDeleted: suspend (CarEvent) -> Unit = {},
) {
    route("/cars") {
        get {
            val cars = carService.getAllCars()
                .map { it.toResponse() }
            call.respond(HttpStatusCode.OK, cars)
        }

        get("/{id}") {
            val id = call.parameters["id"]
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing car id")
                return@get
            }
            val car = carService.getCar(id)
            if (car == null) {
                call.respond(HttpStatusCode.NotFound, "No Car with id $id found")
            } else {
                call.respond(HttpStatusCode.OK, car.toResponse())
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
            } else {
                call.respond(HttpStatusCode.OK, updatedCar.toResponse())
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