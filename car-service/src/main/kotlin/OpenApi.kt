package at.ac.hcw

import io.github.smiley4.ktorswaggerui.SwaggerUI
import io.github.smiley4.ktorswaggerui.routing.openApiSpec
import io.github.smiley4.ktorswaggerui.routing.swaggerUI
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureOpenApi() {
    install(SwaggerUI) {
        info {
            title = "Car Rental Service API"
            version = "0.0.1"
            description = "REST API for the Car Rental Service"
        }
    }
    routing {
        route("/openapi.json") {
            openApiSpec()
        }
        route("/swagger") {
            swaggerUI("/openapi.json")
        }
    }
}