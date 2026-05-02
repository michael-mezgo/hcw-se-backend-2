package at.ac.hcw.routes

import at.ac.hcw.database.toEvent
import at.ac.hcw.database.toResponse
import at.ac.hcw.dto.*
import at.ac.hcw.service.UserService
import com.mongodb.MongoException
import io.github.smiley4.ktorswaggerui.dsl.routing.delete
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.patch
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.adminRoutes(
    userService: UserService,
    onUserCreated: suspend (UserEvent) -> Unit = {},
    onUserDeleted: suspend (UserEvent) -> Unit = {}
) {

    authenticate("admin-jwt") {

        route("/users") {

            // GET ALL USERS
            get({
                tags("Admin")
                summary = "Get all users"
                description = "Returns all users in the system. Requires admin privileges."

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
                try {
                    val users = userService.findAll()
                    call.respond(HttpStatusCode.OK, users.map { it.toResponse() })
                } catch (e: MongoException) {
                    call.respond(HttpStatusCode.ServiceUnavailable, e.message ?: "Database Error!")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, e.message ?: "Unknown Error! Contact Admin!")
                    println(e.message)
                }
            }

            // CREATE USER
            post({
                tags("Admin")
                summary = "Create user"
                description = "Creates a new user. Admins can assign admin privileges."

                request {
                    body<AdminUserCreate> {
                        description = "User data"
                        required = true
                    }
                }

                response {
                    HttpStatusCode.Created to {
                        description = "User created successfully"
                        body<Map<String, String>>()
                    }
                    HttpStatusCode.Conflict to {
                        description = "Username or email already exists"
                    }
                    HttpStatusCode.Forbidden to {
                        description = "Admin privileges required"
                    }
                }
            }) {
                try {
                    val dto = call.receive<AdminUserCreate>()
                    val user = userService.adminCreate(dto)

                    onUserCreated(user.toEvent())

                    call.respond(HttpStatusCode.Created, mapOf("id" to user.id))
                } catch (e: MongoException) {
                    call.respond(HttpStatusCode.ServiceUnavailable, e.message ?: "Database Error!")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, e.message ?: "Unknown Error! Contact Admin!")
                    println(e.message)
                }
            }

            // GET USER BY ID
            get("/{id}", {
                tags("Admin")
                summary = "Get user by ID"
                description = "Returns a specific user by ID. Requires admin privileges."

                request {
                    pathParameter<String>("id") {
                        description = "User ID"
                        required = true
                    }
                }

                response {
                    HttpStatusCode.OK to {
                        description = "User found"
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
                try {
                    val id = call.parameters["id"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing id")

                    val user = userService.findById(id)
                        ?: return@get call.respond(HttpStatusCode.NotFound)

                    call.respond(HttpStatusCode.OK, user.toResponse())
                } catch (e: MongoException) {
                    call.respond(HttpStatusCode.ServiceUnavailable, e.message ?: "Database Error!")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, e.message ?: "Unknown Error! Contact Admin!")
                    println(e.message)
                }
            }

            // UPDATE USER
            patch("/{id}", {
                tags("Admin")
                summary = "Update user"
                description = "Updates any user. Requires admin privileges."

                request {
                    pathParameter<String>("id") {
                        description = "User ID"
                        required = true
                    }
                    body<AdminUserUpdate> {
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
                    HttpStatusCode.BadRequest to {
                        description = "Missing or invalid ID"
                    }
                    HttpStatusCode.Forbidden to {
                        description = "Admin privileges required"
                    }
                }
            }) {
                try {
                    val id = call.parameters["id"]
                        ?: return@patch call.respond(HttpStatusCode.BadRequest, "Missing id")

                    val dto = call.receive<AdminUserUpdate>()

                    val updated = userService.adminUpdate(id, dto)
                        ?: return@patch call.respond(HttpStatusCode.NotFound)

                    call.respond(HttpStatusCode.OK, updated.toResponse())
                } catch (e: MongoException) {
                    call.respond(HttpStatusCode.ServiceUnavailable, e.message ?: "Database Error!")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, e.message ?: "Unknown Error! Contact Admin!")
                    println(e.message)
                }
            }

            // DELETE USER
            delete("/{id}", {
                tags("Admin")
                summary = "Delete user"
                description = "Deletes a user by ID. Admins cannot delete themselves."

                request {
                    pathParameter<String>("id") {
                        description = "User ID"
                        required = true
                    }
                }

                response {
                    HttpStatusCode.NoContent to {
                        description = "User deleted successfully"
                    }
                    HttpStatusCode.NotFound to {
                        description = "User not found"
                    }
                    HttpStatusCode.BadRequest to {
                        description = "Missing or invalid ID"
                    }
                    HttpStatusCode.Forbidden to {
                        description = "Admin cannot delete themselves or lacks permissions"
                    }
                }
            }) {
                try {
                    val principal = call.principal<JWTPrincipal>()!!
                    val adminId = principal.payload.getClaim("userId").asString()

                    val id = call.parameters["id"]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing id")

                    if (id == adminId) {
                        return@delete call.respond(
                            HttpStatusCode.Forbidden,
                            "Admins cannot delete themselves"
                        )
                    }

                    val deleted = userService.delete(id)
                        ?: return@delete call.respond(HttpStatusCode.NotFound)

                    onUserDeleted(deleted.toEvent())

                    call.respond(HttpStatusCode.NoContent)
                } catch (e: MongoException) {
                    call.respond(HttpStatusCode.ServiceUnavailable, e.message ?: "Database Error!")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, e.message ?: "Unknown Error! Contact Admin!")
                    println(e.message)
                }
            }
        }
    }
}