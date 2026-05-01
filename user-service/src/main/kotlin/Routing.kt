package at.ac.hcw

import at.ac.hcw.routes.adminRoutes
import at.ac.hcw.routes.userRoutes
import at.ac.hcw.dto.UserEvent
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.basicPublish
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.rabbitmq
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun Application.configureRouting() {
    val app = this
    val userService = attributes[UserServiceKey]
    val authService = attributes[AuthServiceKey]

    fun publishUserEvent(event: UserEvent, routingKey: String) {
        app.rabbitmq {
            basicPublish {
                exchange = "user-events"
                this.routingKey = routingKey
                message { Json.encodeToString(event) }
            }
        }
    }

    routing {

        route("/api"){
            userRoutes(

                userService = userService,
                authService = authService,

                onUserCreated = { event -> publishUserEvent(event, "user.created") },

                onUserDeleted = { event -> publishUserEvent(event, "user.deleted") }
            )

            adminRoutes(

                userService = userService,

                onUserCreated = { event -> publishUserEvent(event, "user.created") },

                onUserDeleted = { event -> publishUserEvent(event, "user.deleted") }
            )
        }
    }
}