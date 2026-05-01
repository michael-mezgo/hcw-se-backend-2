package at.ac.hcw.routes

import at.ac.hcw.CurrencyClient
import at.ac.hcw.at.ac.hcw.service.BlobStorageService
import at.ac.hcw.dto.*
import at.ac.hcw.service.CarService
import io.github.smiley4.ktorswaggerui.dsl.routing.delete
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.patch
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.ktor.http.*
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.naming.ServiceUnavailableException

fun Route.carRoutes(
    carService: CarService,
    currencyClient: CurrencyClient? = null,
    onCarCreated: suspend (CarEvent) -> Unit = {},
    onCarDeleted: suspend (CarEvent) -> Unit = {},
    blobStorageClient: BlobStorageService? = null
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
                HttpStatusCode.InternalServerError to {
                    description = "Unknown Error!"
                }
                HttpStatusCode.ServiceUnavailable to {description = "Service unavailable"}
            }
        }) {
            val currency = call.request.queryParameters["currency"] ?: "USD"

            try {
                val cars = carService.getAllCars()
                    .map { it.toResponse(currencyClient, currency) }

                call.respond(HttpStatusCode.OK, cars)
            } catch (e: BadRequestException) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "")
            } catch (e: ServiceUnavailableException) {
                call.respond(HttpStatusCode.ServiceUnavailable, e.message ?: "Currency service unavailable")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Unknown Error! Contact Admin!")
                println(e.message)
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
                HttpStatusCode.ServiceUnavailable to {description = "Service unavailable"}
                HttpStatusCode.InternalServerError to {
                    description = "Unknown Error!"
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
            } catch (e: ServiceUnavailableException) {
                call.respond(HttpStatusCode.ServiceUnavailable, e.message ?: "Currency service unavailable")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Unknown Error! Contact Admin!")
                println(e.message)
            }
        }

        authenticate ("admin-jwt") {
            post({
                tags("Cars")
                summary = "Create a new car"
                description = "Creates a new car. This endpoint is intended for admins."

                request {
                    multipartBody {
                        description = "Multipart form data with car details and image"
                        mediaTypes(ContentType.MultiPart.FormData)
                        part<String>("data") {
                            mediaTypes(ContentType.Application.Json)
                        }
                        part<ByteArray>("image") {
                            mediaTypes(ContentType.Image.Any)
                        }
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
                    HttpStatusCode.InternalServerError to {
                        description = "Unknown Error!"
                    }
                }
            }) {
                if (blobStorageClient == null) {
                    call.respond(HttpStatusCode.ServiceUnavailable, "Blob storage service not available")
                    return@post
                }

                var carData: CarCreateRequest? = null
                var imageUrl: String? = null

                call.receiveMultipart().forEachPart { part ->
                    when {
                        part is PartData.FormItem && part.name == "data" -> {
                            carData = Json.decodeFromString(part.value)
                        }
                        part is PartData.FileItem && part.name == "image" -> {
                            val bytes = part.provider().readRemaining().readByteArray()
                            val ext = part.originalFileName?.substringAfterLast('.', "jpg") ?: "jpg"
                            val blobName = "cars/${UUID.randomUUID()}.$ext"
                            blobStorageClient.upload(blobName, bytes)
                            imageUrl = blobName
                        }
                    }
                    part.dispose()
                }

                if (carData == null) {
                    call.respond(HttpStatusCode.BadRequest, "Missing car data")
                    return@post
                } else if (imageUrl == null) {
                    call.respond(HttpStatusCode.BadRequest, "Missing image url")
                    return@post
                } else {
                    val createdId = carService.createCar(carData!!.toDomain(imageUrl))
                    onCarCreated(CarEvent(createdId))
                    call.respond(HttpStatusCode.Created, mapOf("id" to createdId))
                }
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
                    multipartBody {
                        description = "Multipart form data with updated car details and optional image"
                        mediaTypes(ContentType.MultiPart.FormData)
                        part<String>("data") {
                            mediaTypes(ContentType.Application.Json)
                        }
                        part<ByteArray>("image") {
                            mediaTypes(ContentType.Image.Any)
                        }
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
                    HttpStatusCode.InternalServerError to {
                        description = "Unknown Error!"
                    }
                }
            }) {
                val id = call.parameters["id"]

                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Missing car id")
                    return@patch
                }

                if (blobStorageClient == null) {
                    call.respond(HttpStatusCode.ServiceUnavailable, "Blob storage service not available")
                    return@patch
                }

                var carData: CarPatchRequest? = null
                var imageUrl: String? = null

                call.receiveMultipart().forEachPart { part ->
                    when {
                        part is PartData.FormItem && part.name == "data" -> {
                            carData = Json.decodeFromString(part.value)
                        }
                        part is PartData.FileItem && part.name == "image" -> {
                            val bytes = part.provider().readRemaining().readByteArray()
                            val ext = part.originalFileName?.substringAfterLast('.', "jpg") ?: "jpg"
                            val blobName = "cars/${UUID.randomUUID()}.$ext"
                            blobStorageClient.upload(blobName, bytes)
                            imageUrl = blobName
                        }
                    }
                    part.dispose()
                }


                try {
                    val dto = carData ?: throw BadRequestException("Missing car data")
                    if (imageUrl != null) {
                        val oldImage = carService.getCar(id)?.imageName?.takeIf { it.isNotBlank() }
                        carService.patchCar(id, dto.copy(imageName = imageUrl))
                        if (oldImage != null) blobStorageClient.delete(oldImage)
                    } else {
                        carService.patchCar(id, dto)
                    }
                } catch (e: BadRequestException) {
                    call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, e.message ?: "Unknown Error! Contact Admin!")
                    println(e.message)
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
                    HttpStatusCode.InternalServerError to {
                        description = "Unknown Error!"
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