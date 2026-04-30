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
            }) {
                try {
                    val principal = call.principal<JwtPrincipal>()!!

                    val id = call.parameters["id"]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing id")

                    if (id == principal.userId) {
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