package at.ac.hcw

import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.resources.*

fun Application.configureRouting() {
    routing {
        get("/", {
            tags("General")
            description = "Hello World endpoint"
            response {
                HttpStatusCode.OK to {
                    description = "Returns a greeting text"
                    body<String>()
                }
            }
        }) {
            call.respondText("Hello, World!")
        }

        get<Articles> { article ->
            // Get all articles ...
            call.respond("List of articles sorted starting from ${article.sort}")
        }

        get("/json/kotlinx-serialization", {
            tags("JSON")
            description = "Example JSON response using kotlinx serialization"
            response {
                HttpStatusCode.OK to {
                    description = "A simple JSON object"
                    body<Map<String, String>>()
                }
            }
        }) {
            call.respond(mapOf("hello" to "world"))
        }
    }
}
