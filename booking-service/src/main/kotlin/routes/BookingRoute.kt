package at.ac.hcw.routes

import at.ac.hcw.JwtPrincipal
import at.ac.hcw.dto.*
import at.ac.hcw.exceptions.CarAlreadyBookedException
import at.ac.hcw.exceptions.CarNotFoundException
import at.ac.hcw.exceptions.UserNotFoundException
import at.ac.hcw.model.toCancelledEvent
import at.ac.hcw.model.toCreatedEvent
import at.ac.hcw.model.toResponse
import at.ac.hcw.service.BookingService
import io.github.smiley4.ktorswaggerui.dsl.routing.delete
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.bookingRoutes(
    bookingService: BookingService,
    onBookingCreated: suspend (BookingCreatedEvent) -> Unit = {},
    onBookingCancelled: suspend (BookingCancelledEvent) -> Unit = {}
) {
    route("/bookings") {
        authenticate("user-jwt") {
            post({
                tags("Bookings")
                description = "Create a new booking for a car"
                request {
                    body<BookingRequest> {
                        description = "userId and carId to book"
                        required = true
                    }
                }
                response {
                    HttpStatusCode.Created to {
                        description = "Booking created successfully"
                        body<BookingResponse>()
                    }
                    HttpStatusCode.NotFound to { description = "User or car not found" }
                    HttpStatusCode.Conflict to { description = "Car is already booked" }
                }
            }) {
                try {
                    val request = call.receive<BookingRequest>()
                    val principal = call.principal<JwtPrincipal>()!!

                    if(principal.userId != request.userId)
                        call.respond(HttpStatusCode.Forbidden, "User doesn't have permission to create bookings for another user")

                    val booking = bookingService.create(request)
                    onBookingCreated(booking.toCreatedEvent())
                    call.respond(HttpStatusCode.Created, booking.toResponse())
                } catch (e: UserNotFoundException) {
                    call.respond(HttpStatusCode.NotFound, e.message ?: "User not found")
                } catch (e: CarNotFoundException) {
                    call.respond(HttpStatusCode.NotFound, e.message ?: "Car not found")
                } catch (e: CarAlreadyBookedException) {
                    call.respond(HttpStatusCode.Conflict, e.message ?: "Car is already booked")
                }
            }
        }

        authenticate("user-jwt", "admin-jwt") {
            delete("/{id}", {
                tags("Bookings")
                description = "Cancel an existing booking"
                request {
                    pathParameter<String>("id") {
                        description = "The booking ID"
                        required = true
                    }
                }
                response {
                    HttpStatusCode.OK to {
                        description = "Booking cancelled successfully"
                        body<BookingResponse>()
                    }
                    HttpStatusCode.NotFound to { description = "Booking not found" }
                    HttpStatusCode.BadRequest to { description = "Missing or invalid id" }
                    HttpStatusCode.Forbidden to { description = "Not allowed to cancel another user's booking" }
                }
            }) {
                val id = call.parameters["id"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing id")

                val principal = call.principal<JwtPrincipal>()!!
                val booking = bookingService.findById(id)
                    ?: return@delete call.respond(HttpStatusCode.NotFound, "Booking not found")

                if (!principal.isAdmin && booking.userId != principal.userId) {
                    return@delete call.respond(HttpStatusCode.Forbidden, "Not allowed to cancel another user's booking")
                }

                val cancelled = bookingService.cancel(id)!!
                onBookingCancelled(cancelled.toCancelledEvent())
                call.respond(HttpStatusCode.OK, cancelled.toResponse())
            }
        }

        authenticate("user-jwt", "admin-jwt") {
            get({
                tags("Bookings")
                description = "Get all bookings for a specific user"
                request {
                    queryParameter<String>("userId") {
                        description = "The user ID to filter bookings"
                        required = true
                    }
                }
                response {
                    HttpStatusCode.OK to {
                        description = "List of bookings"
                        body<List<BookingResponse>>()
                    }
                    HttpStatusCode.BadRequest to { description = "Missing userId query parameter" }
                    HttpStatusCode.Forbidden to { description = "Not allowed to view bookings of another user" }
                }
            }) {
                val userId = call.request.queryParameters["userId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing userId query parameter")

                val principal = call.principal<JwtPrincipal>()!!

                if (!principal.isAdmin && principal.userId != userId) {
                    return@get call.respond(HttpStatusCode.Forbidden, "Not allowed to view bookings of another user")
                }

                call.respond(HttpStatusCode.OK, bookingService.findByUser(userId).map { it.toResponse() })
            }
        }
    }
}
