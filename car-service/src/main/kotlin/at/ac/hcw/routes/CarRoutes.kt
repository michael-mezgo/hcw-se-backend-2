package at.ac.hcw.routes

import at.ac.hcw.dto.*
import at.ac.hcw.service.CarService
import currency.CurrencyServiceGrpcKt
import io.github.smiley4.ktorswaggerui.dsl.routing.*
import io.ktor.http.*
import io.ktor.server.auth.authenticate
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
    route("/cars") {
        get({
            tags("Cars")
            summary = "List all cars"
            description = "Returns all cars. Optionally converts the daily price to the requested currency."

            request {
                queryParameter<String>("currency") {
                    description = "Optional target currency for price conversion. Defaults to USD."
                }
            }

            response {
                HttpStatusCode.OK to {
                    description = "List of cars"
                    body<List<CarResponse>>()
                }
                HttpStatusCode.BadRequest to {
                    description = "Invalid or unsupported currency"
                }
            }
        }) {
            val currency = call.request.queryParameters["currency"] ?: "USD"

            try {
                val cars = carService.getAllCars()
                    .map { it.toResponse(currencyClient, currency) }

                call.respond(HttpStatusCode.OK, cars)
            } catch (e: BadRequestException) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "")
            }
        }

        get("/{id}", {
            tags("Cars")
            summary = "Get car details"
            description = "Returns details for a specific car by its MongoDB ObjectId."

            request {
                pathParameter<String>("id") {
                    description = "MongoDB ObjectId of the car"
                }
                queryParameter<String>("currency") {
                    description = "Optional target currency for price conversion. Defaults to USD."
                }
            }

            response {
                HttpStatusCode.OK to {
                    description = "Car details"
                    body<CarResponse>()
                }
                HttpStatusCode.BadRequest to {
                    description = "Missing car id or invalid currency"
                }
                HttpStatusCode.NotFound to {
                    description = "Car not found"
                }
            }
        }) {
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

        authenticate ("admin-jwt") {
            post({
                tags("Cars")
                summary = "Create a new car"
                description = "Creates a new car. This endpoint is intended for admins."

                request {
                    body<CarCreateRequest> {
                        description = "Car data for the new car"
                    }
                }

                response {
                    HttpStatusCode.Created to {
                        description = "Car created successfully. The response contains the created car id."
                        body<Map<String, String>>()
                    }
                    HttpStatusCode.BadRequest to {
                        description = "Invalid request body"
                    }
                    HttpStatusCode.Forbidden to {
                        description = "JSON Web Token with admin privileges required"
                    }
                }
            }) {
                val request = call.receive<CarCreateRequest>()
                val createdId = carService.createCar(request.toDomain())

                onCarCreated(CarEvent(createdId))

                call.respond(HttpStatusCode.Created, mapOf("id" to createdId))
            }
        }


        authenticate ("admin-jwt") {
            patch("/{id}", {
                tags("Cars")
                summary = "Update car details"
                description =
                    "Partially updates a car. Only provided fields are changed. This endpoint is intended for admins."

                request {
                    pathParameter<String>("id") {
                        description = "MongoDB ObjectId of the car"
                    }
                    body<CarPatchRequest> {
                        description = "Fields that should be updated"
                    }
                }

                response {
                    HttpStatusCode.OK to {
                        description = "Updated car"
                        body<CarResponse>()
                    }
                    HttpStatusCode.BadRequest to {
                        description = "Missing car id, invalid request body, or invalid currency"
                    }
                    HttpStatusCode.NotFound to {
                        description = "Car not found"
                    }
                    HttpStatusCode.Forbidden to {
                        description = "JSON Web Token with admin privileges required"
                    }
                }
            }) {
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
        }

        authenticate ("admin-jwt") {
            delete("/{id}", {
                tags("Cars")
                summary = "Delete a car"
                description = "Deletes a car by its MongoDB ObjectId. This endpoint is intended for admins."

                request {
                    pathParameter<String>("id") {
                        description = "MongoDB ObjectId of the car"
                    }
                }

                response {
                    HttpStatusCode.NoContent to {
                        description = "Car deleted successfully"
                    }
                    HttpStatusCode.BadRequest to {
                        description = "Missing car id"
                    }
                    HttpStatusCode.NotFound to {
                        description = "Car not found"
                    }
                    HttpStatusCode.Forbidden to {
                        description = "JSON Web Token with admin privileges required"
                    }
                }
            }) {
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
}