package at.ac.hcw

import at.ac.hcw.routes.userRoutes
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.basicPublish
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.rabbitmq
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun Application.configureRouting() {
    val app = this
    val userService = attributes[UserServiceKey]
    val authService = attributes[AuthServiceKey]
    routing {
        userRoutes(

            userService = userService,
            authService = authService,

            onUserCreated = { event ->
                app.rabbitmq {
                    basicPublish {
                        exchange = "user-events"
                        routingKey = "user.created"
                        message {
                            Json.encodeToString(event)
                        }
                    }
                }
            },

            onUserDeleted = { event ->
                app.rabbitmq {
                    basicPublish {
                        exchange = "user-events"
                        routingKey = "user.deleted"
                        message {
                            Json.encodeToString(event)
                        }
                    }
                }
            }
        )
    }
}