package at.ac.hcw

import at.ac.hcw.routes.carRoutes
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.basicPublish
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.rabbitmq
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    val app = this
    val carService = attributes[CarServiceKey]
    val currencyClient = attributes[CurrencyClientKey]
    val carBlobStorageClient = attributes[CarBlobContainerClientKey]
    routing {
        route("/api") {
            carRoutes(
                carService = carService,
                currencyClient = currencyClient,
                onCarCreated = { carEvent ->
                    app.rabbitmq {
                        basicPublish {
                            exchange = "car-events"
                            routingKey = "car.created"
                            message { carEvent }
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
                blobStorageClient = carBlobStorageClient
            )
        }
    }
}