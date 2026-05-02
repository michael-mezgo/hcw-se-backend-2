package at.ac.hcw

import at.ac.hcw.routes.bookingRoutes
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.basicPublish
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.rabbitmq
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    val bookingService = attributes[BookingServiceKey]
    val app = this

    routing {
        route("/api") {
            bookingRoutes(
                bookingService = bookingService,
                onBookingCreated = { event ->
                    app.rabbitmq {
                        basicPublish {
                            exchange = "booking-events"
                            routingKey = "booking.created"
                            message { event }
                        }
                    }
                },
                onBookingCancelled = { event ->
                    app.rabbitmq {
                        basicPublish {
                            exchange = "booking-events"
                            routingKey = "booking.deleted"
                            message { event }
                        }
                    }
                }
            )
        }
    }
}
