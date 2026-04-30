package at.ac.hcw

import at.ac.hcw.routes.carRoutes
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.basicPublish
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.rabbitmq
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun Application.configureRouting() {
    val app = this
    val carService = attributes[CarServiceKey]
    val currencyClient = attributes[CurrencyClientKey]
    routing {
        route("/api") {
            carRoutes(
                onCarCreated = { carEvent ->
                    app.rabbitmq {
                        basicPublish {
                            exchange = "car-events"
                            routingKey = "car.created"
                            message { Json.encodeToString(carEvent)}
                        }
                    }
                },
                onCarDeleted = { carEvent ->
                    app.rabbitmq {
                        basicPublish {
                            exchange = "car-events"
                            routingKey = "car.deleted"
                            message { Json.encodeToString(carEvent) }
                        }
                    }
                },
                carService = carService,
                currencyClient = currencyClient
            )
        }
    }
}