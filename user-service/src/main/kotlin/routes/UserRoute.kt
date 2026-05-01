package at.ac.hcw.routes

import at.ac.hcw.database.toEvent
import at.ac.hcw.database.toResponse
import at.ac.hcw.dto.*
import at.ac.hcw.exceptions.UserExistsException
import at.ac.hcw.service.AuthService
import at.ac.hcw.service.UserService
import com.mongodb.MongoException
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
                HttpStatusCode.ServiceUnavailable to {
                    description = "Service unavailable"
                }
                HttpStatusCode.InternalServerError to {
                    description = "Unknown Error!"
                }
            }
        }) {
            try {
                val registration = call.receive<UserRegistration>()
                val user = authService.register(registration)

                onUserCreated(user.toEvent())

                call.respond(HttpStatusCode.Created, mapOf("id" to user.id.toHexString()))
            } catch (e: UserExistsException) {
                call.respond(HttpStatusCode.Conflict, e.message ?: "User already exists!")
            } catch (e: MongoException) {
                call.respond(HttpStatusCode.ServiceUnavailable, e.message ?: "Database Error!")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Unknown Error! Contact Admin!")
                println(e.message)
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
                HttpStatusCode.ServiceUnavailable to {
                    description = "Service unavailable"
                }
                HttpStatusCode.InternalServerError to {
                    description = "Unknown Error!"
                }
            }
        }) {
            try {
                val credentials = call.receive<UserLoginRequest>()
                val response = authService.login(credentials)
                call.respond(HttpStatusCode.OK, response)
            } catch (e: MongoException) {
                call.respond(HttpStatusCode.ServiceUnavailable, e.message ?: "Database Error!")
            }catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Unknown Error! Contact Admin!")
                println(e.message)
            }
        }
    }

    // ── USER ─────────────────────────────────────────────
    authenticate("user-jwt") {

        route("/users/me") {

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
                    HttpStatusCode.ServiceUnavailable to {
                        description = "Service unavailable"
                    }
                    HttpStatusCode.InternalServerError to {
                        description = "Unknown Error!"
                    }
                }
            }) {
                try {
                    val principal = call.principal<JwtPrincipal>()!!
                    val user = userService.findById(principal.userId)
                        ?: return@get call.respond(HttpStatusCode.NotFound)

                    call.respond(HttpStatusCode.OK, user.toResponse())
                } catch (e: MongoException) {
                    call.respond(HttpStatusCode.ServiceUnavailable, e.message ?: "Database Error!")
                }catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, e.message ?: "Unknown Error! Contact Admin!")
                    println(e.message)
                }
            }

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
                    HttpStatusCode.ServiceUnavailable to {
                        description = "Service unavailable"
                    }
                    HttpStatusCode.InternalServerError to {
                        description = "Unknown Error!"
                    }
                }
            }) {
                try {
                    val principal = call.principal<JwtPrincipal>()!!
                    val update = call.receive<UserUpdate>()

                    val updated = userService.update(principal.userId, update)
                        ?: return@patch call.respond(HttpStatusCode.NotFound)

                    call.respond(HttpStatusCode.OK, updated.toResponse())
                } catch (e: MongoException) {
                    call.respond(HttpStatusCode.ServiceUnavailable, e.message ?: "Database Error!")
                }catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, e.message ?: "Unknown Error! Contact Admin!")
                    println(e.message)
                }
            }

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
                    HttpStatusCode.ServiceUnavailable to {
                        description = "Service unavailable"
                    }
                    HttpStatusCode.InternalServerError to {
                        description = "Unknown Error!"
                    }
                }
            }) {
                try {
                    val principal = call.principal<JwtPrincipal>()!!

                    val deleted = userService.delete(principal.userId)
                        ?: return@delete call.respond(HttpStatusCode.NotFound)

                    onUserDeleted(deleted.toEvent())

                    call.respond(HttpStatusCode.NoContent)
                } catch (e: MongoException) {
                    call.respond(HttpStatusCode.ServiceUnavailable, e.message ?: "Database Error!")
                }catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, e.message ?: "Unknown Error! Contact Admin!")
                    println(e.message)
                }
            }
        }
    }

}