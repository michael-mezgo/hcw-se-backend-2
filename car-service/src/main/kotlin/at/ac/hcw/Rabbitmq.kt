package at.ac.hcw

import at.ac.hcw.dto.BookingCreatedEvent
import at.ac.hcw.dto.BookingCancelledEvent
import io.github.damir.denis.tudor.ktor.server.rabbitmq.RabbitMQ
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.*
import io.github.damir.denis.tudor.ktor.server.rabbitmq.rabbitMQ
import io.ktor.server.application.*
import io.ktor.server.config.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json

fun Application.configureRabbitmq() {
    val carService = attributes[CarServiceKey]
    val exceptionHandler = CoroutineExceptionHandler { _, throwable -> log.error("ExceptionHandler got $throwable") }
    val rabbitMQScope = CoroutineScope(SupervisorJob() + exceptionHandler)

    install(RabbitMQ) {
        uri = environment.config.tryGetString("rabbitmq.uri").also {
            if (it == null) log.warn("rabbitmq.uri not configured, using default: amqp://guest:guest@localhost:5672")
        } ?: "amqp://guest:guest@localhost:5672"
        defaultConnectionName = "default-connection"
        dispatcherThreadPollSize = 4
        tlsEnabled = false
        scope = rabbitMQScope // custom scope, default is the one provided by Ktor
    }

    rabbitmq { exchangeDeclare { exchange = "car-events"; type = "topic"; durable = true } }
    rabbitmq { exchangeDeclare { exchange = "booking-events"; type = "topic"; durable = true } }

// Bind queues for booking events
    rabbitmq {
        queueBind {
            queue = "car-service.booking.created"
            exchange = "booking-events"
            routingKey = "booking.created"
            queueDeclare { queue = "car-service.booking.created"; durable = true }
        }
    }
    rabbitmq {
        queueBind {
            queue = "car-service.booking.deleted"
            exchange = "booking-events"
            routingKey = "booking.deleted"
            queueDeclare { queue = "car-service.booking.deleted"; durable = true }
        }
    }

    // Consume booking.created
    val eventJson = Json {
        ignoreUnknownKeys = true
    }
    val consumerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    monitor.subscribe(ApplicationStopped) {
        consumerScope.cancel()
    }
    rabbitmq {
        basicConsume {
            autoAck = true
            queue = "car-service.booking.created"
            dispatcher = Dispatchers.rabbitMQ
            deliverCallback<String> { message ->
                val event = eventJson.decodeFromString<BookingCreatedEvent>(message.body)
                consumerScope.launch {
                    val success = carService.markCarAsBooked(event.carId)
                    if (success) {
                        log.info("Car is booked: ${event.carId}")
                    } else {
                        log.warn("Failed to mark car as booked: ${event.carId}")
                    }
                }
            }
        }
    }

// Consume user.deleted
    rabbitmq {
        basicConsume {
            autoAck = true
            queue = "car-service.booking.deleted"
            dispatcher = Dispatchers.rabbitMQ
            deliverCallback<String> { message ->
                val event = eventJson.decodeFromString<BookingCancelledEvent>(message.body)
                consumerScope.launch {
                    val success = carService.markCarAsAvailable(event.carId)
                    if (success) {
                        log.info("Car is available: ${event.carId}")
                    } else {
                        log.warn("Failed to mark car as available: ${event.carId}")
                    }
                }
            }
        }
    }
}
