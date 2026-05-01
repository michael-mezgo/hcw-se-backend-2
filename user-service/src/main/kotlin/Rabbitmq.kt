package at.ac.hcw

import io.github.damir.denis.tudor.ktor.server.rabbitmq.RabbitMQ
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.*
import io.ktor.server.application.*
import io.ktor.server.config.tryGetString
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

fun Application.configureRabbitmq() {
    val exceptionHandler = CoroutineExceptionHandler { _, throwable -> log.error("ExceptionHandler got $throwable") }
    val rabbitMQScope = CoroutineScope(SupervisorJob() + exceptionHandler)

    install(RabbitMQ) {
        val configUri = environment.config.tryGetString("rabbitmq.uri")
        if (configUri == null) {
            log.warn("RABBITMQ_URI not configured, using default demo URI (amqp://guest:guest@localhost:5672)")
        }
        uri = configUri ?: "amqp://guest:guest@localhost:5672"
        defaultConnectionName = "default-connection"
        dispatcherThreadPollSize = 4
        tlsEnabled = false
        scope = rabbitMQScope // custom scope, default is the one provided by Ktor
    }

    rabbitmq { exchangeDeclare { exchange = "user-events"; type = "topic"; durable = true } }

}
