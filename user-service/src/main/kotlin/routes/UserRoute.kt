package at.ac.hcw.routes

import at.ac.hcw.database.toEvent
import at.ac.hcw.database.toResponse
import at.ac.hcw.dto.*
import at.ac.hcw.service.AuthService
import at.ac.hcw.service.UserService
import io.github.smiley4.ktorswaggerui.dsl.routing.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRoutes(
    userService: UserService,
    authService: AuthService,
    onUserCreated: suspend (UserEvent) -> Unit = {},
    onUserDeleted: suspend (UserEvent) -> Unit = {}
) {

    // ── AUTH ─────────────────────────────────────────────
    route("/auth") {

        post("/register", {
            tags("Auth")
            summary = "Register a new user"
            request { body<UserRegistration>() }
            response {
                HttpStatusCode.Created to { body<Map<String, String>>() }
                HttpStatusCode.Conflict to {}
            }
        }) {
            try {
                val registration = call.receive<UserRegistration>()
                val user = authService.register(registration)

                onUserCreated(user.toEvent())

                call.respond(HttpStatusCode.Created, mapOf("id" to user.id))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.Conflict, e.message ?: "User already exists")
            }
        }

        post("/login", {
            tags("Auth")
        }) {
            val credentials = call.receive<UserLoginRequest>()
            val response = authService.login(credentials)
            call.respond(HttpStatusCode.OK, response)
        }
    }

    // ── USER ─────────────────────────────────────────────
    authenticate("user-jwt") {

        route("/users/me") {

            // GET PROFILE
            get({
                tags("Users")
            }) {
                val principal = call.principal<JwtPrincipal>()!!
                val user = userService.findById(principal.userId)
                    ?: return@get call.respond(HttpStatusCode.NotFound)

                call.respond(HttpStatusCode.OK, user.toResponse())
            }

            // UPDATE PROFILE
            patch({
                tags("Users")
            }) {
                val principal = call.principal<JwtPrincipal>()!!
                val update = call.receive<UserUpdate>()

                val updated = userService.update(principal.userId, update)
                    ?: return@patch call.respond(HttpStatusCode.NotFound)

                call.respond(HttpStatusCode.OK, updated.toResponse())
            }

            // DELETE (Soft oder Hard)
            delete({
                tags("Users")
            }) {
                val principal = call.principal<JwtPrincipal>()!!

                val deleted = userService.delete(principal.userId)
                    ?: return@delete call.respond(HttpStatusCode.NotFound)

                onUserDeleted(deleted.toEvent())

                call.respond(HttpStatusCode.NoContent)
            }
        }
    }

    // ── ADMIN OPTIONAL ───────────────────────────────────
    authenticate("admin-jwt") {

        route("/users") {

            get({
                tags("Users")
                description = "Get all users (admin only)"
            }) {
                val users = userService.findAll()
                call.respond(HttpStatusCode.OK, users.map { it.toResponse() })
            }

            delete("/{id}", {
                tags("Users")
                description = "Delete any user (admin only)"
            }) {
                val id = call.parameters["id"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing id")

                val deleted = userService.delete(id)
                    ?: return@delete call.respond(HttpStatusCode.NotFound)

                onUserDeleted(deleted.toEvent())

                call.respond(HttpStatusCode.OK, deleted.toResponse())
            }
        }
    }
}