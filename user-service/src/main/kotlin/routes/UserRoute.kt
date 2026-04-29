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
            description = "Creates a new user account."

            request {
                body<UserRegistration> {
                    description = "User registration data"
                    required = true
                }
            }

            response {
                HttpStatusCode.Created to {
                    description = "User successfully created"
                    body<Map<String, String>>()
                }
                HttpStatusCode.Conflict to {
                    description = "Username or email already exists"
                }
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
            summary = "Login user"
            description = "Authenticates user and returns JWT token."

            request {
                body<UserLoginRequest> {
                    description = "User login credentials"
                    required = true
                }
            }

            response {
                HttpStatusCode.OK to {
                    description = "Login successful"
                    body<LoginResponse>()
                }
                HttpStatusCode.Unauthorized to {
                    description = "Invalid credentials"
                }
            }
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
                summary = "Get own profile"
                description = "Returns the authenticated user's profile."

                response {
                    HttpStatusCode.OK to {
                        description = "User profile"
                        body<UserResponse>()
                    }
                    HttpStatusCode.NotFound to {
                        description = "User not found"
                    }
                    HttpStatusCode.Unauthorized to {
                        description = "Not authenticated"
                    }
                }
            }) {
                val principal = call.principal<JwtPrincipal>()!!
                val user = userService.findById(principal.userId)
                    ?: return@get call.respond(HttpStatusCode.NotFound)

                call.respond(HttpStatusCode.OK, user.toResponse())
            }

            // UPDATE PROFILE
            patch({
                tags("Users")
                summary = "Update own profile"
                description = "Updates user profile fields."

                request {
                    body<UserUpdate> {
                        description = "Fields to update"
                        required = true
                    }
                }

                response {
                    HttpStatusCode.OK to {
                        description = "User updated successfully"
                        body<UserResponse>()
                    }
                    HttpStatusCode.NotFound to {
                        description = "User not found"
                    }
                    HttpStatusCode.Unauthorized to {
                        description = "Not authenticated"
                    }
                }
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
                summary = "Delete own account"
                description = "Deletes the authenticated user's account."

                response {
                    HttpStatusCode.NoContent to {
                        description = "User deleted successfully"
                    }
                    HttpStatusCode.NotFound to {
                        description = "User not found"
                    }
                    HttpStatusCode.Unauthorized to {
                        description = "Not authenticated"
                    }
                }
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
                summary = "Get all users"
                description = "Returns all users (admin only)."

                response {
                    HttpStatusCode.OK to {
                        description = "List of users"
                        body<List<UserResponse>>()
                    }
                    HttpStatusCode.Forbidden to {
                        description = "Admin privileges required"
                    }
                }
            }) {
                val users = userService.findAll()
                call.respond(HttpStatusCode.OK, users.map { it.toResponse() })
            }

            delete("/{id}", {
                tags("Users")
                summary = "Delete user by ID"
                description = "Deletes a user by ID (admin only)."

                request {
                    pathParameter<String>("id") {
                        description = "User ID"
                        required = true
                    }
                }

                response {
                    HttpStatusCode.OK to {
                        description = "User deleted successfully"
                        body<UserResponse>()
                    }
                    HttpStatusCode.NotFound to {
                        description = "User not found"
                    }
                    HttpStatusCode.BadRequest to {
                        description = "Missing or invalid ID"
                    }
                    HttpStatusCode.Forbidden to {
                        description = "Admin privileges required"
                    }
                }
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