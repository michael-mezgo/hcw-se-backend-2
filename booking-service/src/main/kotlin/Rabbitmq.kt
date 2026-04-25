package at.ac.hcw

import at.ac.hcw.dto.CarEvent
import at.ac.hcw.dto.UserEvent
import io.github.damir.denis.tudor.ktor.server.rabbitmq.RabbitMQ
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.*
import io.github.damir.denis.tudor.ktor.server.rabbitmq.rabbitMQ
import io.ktor.server.application.*
import io.ktor.server.config.tryGetString
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json

fun Application.configureRabbitmq() {
    val knownEntities = attributes[KnownEntitiesServiceKey]
    val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        log.error("RabbitMQ error: $throwable")
    }
    val rabbitMQScope = CoroutineScope(SupervisorJob() + exceptionHandler)

    install(RabbitMQ) {
        uri = environment.config.tryGetString("rabbitmq.uri") ?: "amqp://guest:guest@localhost:5672"
        defaultConnectionName = "booking-service-connection"
        dispatcherThreadPollSize = 4
        tlsEnabled = false
        scope = rabbitMQScope
    }

    // Declare exchanges
    rabbitmq { exchangeDeclare { exchange = "user-events"; type = "topic"; durable = true } }
    rabbitmq { exchangeDeclare { exchange = "car-events"; type = "topic"; durable = true } }
    rabbitmq { exchangeDeclare { exchange = "booking-events"; type = "topic"; durable = true } }

    // Bind queues for user events
    rabbitmq {
        queueBind {
            queue = "booking-service.user.created"
            exchange = "user-events"
            routingKey = "user.created"
            queueDeclare { queue = "booking-service.user.created"; durable = true }
        }
    }
    rabbitmq {
        queueBind {
            queue = "booking-service.user.deleted"
            exchange = "user-events"
            routingKey = "user.deleted"
            queueDeclare { queue = "booking-service.user.deleted"; durable = true }
        }
    }

    // Bind queues for car events
    rabbitmq {
        queueBind {
            queue = "booking-service.car.created"
            exchange = "car-events"
            routingKey = "car.created"
            queueDeclare { queue = "booking-service.car.created"; durable = true }
        }
    }
    rabbitmq {
        queueBind {
            queue = "booking-service.car.deleted"
            exchange = "car-events"
            routingKey = "car.deleted"
            queueDeclare { queue = "booking-service.car.deleted"; durable = true }
        }
    }

    // Consume user.created
    rabbitmq {
        basicConsume {
            autoAck = true
            queue = "booking-service.user.created"
            dispatcher = Dispatchers.rabbitMQ
            deliverCallback<String> { message ->
                val event = Json.decodeFromString<UserEvent>(message.body)
                knownEntities.addUser(event.userId)
                log.info("User added to cache: ${event.userId}")
            }
        }
    }

    // Consume user.deleted
    rabbitmq {
        basicConsume {
            autoAck = true
            queue = "booking-service.user.deleted"
            dispatcher = Dispatchers.rabbitMQ
            deliverCallback<String> { message ->
                val event = Json.decodeFromString<UserEvent>(message.body)
                knownEntities.removeUser(event.userId)
                log.info("User removed from cache: ${event.userId}")
            }
        }
    }

    // Consume car.created
    rabbitmq {
        basicConsume {
            autoAck = true
            queue = "booking-service.car.created"
            dispatcher = Dispatchers.rabbitMQ
            deliverCallback<String> { message ->
                val event = Json.decodeFromString<CarEvent>(message.body)
                knownEntities.addCar(event.carId)
                log.info("Car added to cache: ${event.carId}")
            }
        }
    }

    // Consume car.deleted
    rabbitmq {
        basicConsume {
            autoAck = true
            queue = "booking-service.car.deleted"
            dispatcher = Dispatchers.rabbitMQ
            deliverCallback<String> { message ->
                val event = Json.decodeFromString<CarEvent>(message.body)
                knownEntities.removeCar(event.carId)
                log.info("Car removed from cache: ${event.carId}")
            }
        }
    }
}
