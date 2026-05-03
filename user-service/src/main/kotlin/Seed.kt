package at.ac.hcw

import at.ac.hcw.database.toEvent
import at.ac.hcw.dto.AdminUserCreate
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.basicPublish
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.rabbitmq
import io.ktor.server.application.*
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("Seed")

fun Application.configureSeed() {
    val userService = attributes[UserServiceKey]
    val app = this

    val existing = kotlinx.coroutines.runBlocking { userService.findAll() }
    if (existing.isNotEmpty()) return

    log.info("No users found — seeding default admin user")

    val created = kotlinx.coroutines.runBlocking {
        userService.adminCreate(
            AdminUserCreate(
                username = "admin",
                email = "test@test.at",
                password = "admin",
                firstName = "Administrator",
                lastName = "Administrator",
                licenseNumber = "123",
                licenseValidUntil = "2030-12-31",
                isAdmin = true,
            )
        )
    }

    app.rabbitmq {
        basicPublish {
            exchange = "user-events"
            routingKey = "user.created"
            message { created.toEvent() }
        }
    }

    log.info("Admin user created with id=${created.id}")
}
