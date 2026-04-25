package at.ac.hcw

import at.ac.hcw.routes.bookingRoutes
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.basicPublish
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.rabbitmq
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun Application.configureRouting() {
    val bookingService = attributes[BookingServiceKey]
    val app = this

    routing {
        bookingRoutes(
            bookingService = bookingService,
            onBookingCreated = { event ->
                app.rabbitmq {
                    basicPublish {
                        exchange = "booking-events"
                        routingKey = "booking.created"
                        message { Json.encodeToString(event) }
                    }
                }
            },
            onBookingCancelled = { event ->
                app.rabbitmq {
                    basicPublish {
                        exchange = "booking-events"
                        routingKey = "booking.cancelled"
                        message { Json.encodeToString(event) }
                    }
                }
            }
        )
    }
}
