package at.ac.hcw.routes

import at.ac.hcw.database.toEvent
import at.ac.hcw.database.toResponse
import at.ac.hcw.dto.*
import at.ac.hcw.service.UserService
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
    onUserDeleted: suspend (UserEvent) -> Unit = {}
) {

    authenticate("admin-jwt") {

        route("/users") {

            // GET ALL USERS
            get {
                val users = userService.findAll()
                call.respond(HttpStatusCode.OK, users.map { it.toResponse() })
            }

            // CREATE USER (Admin)
            post {
                val dto = call.receive<AdminUserCreate>()

                val user = userService.adminCreate(dto)

                call.respond(
                    HttpStatusCode.Created,
                    mapOf("id" to user.id) // String!
                )
            }

            // GET USER BY ID
            get("/{id}") {
                val id = call.parameters["id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing id")

                val user = userService.findById(id)
                    ?: return@get call.respond(HttpStatusCode.NotFound)

                call.respond(HttpStatusCode.OK, user.toResponse())
            }

            // UPDATE USER
            patch("/{id}") {
                val id = call.parameters["id"]
                    ?: return@patch call.respond(HttpStatusCode.BadRequest, "Missing id")

                val dto = call.receive<AdminUserUpdate>()

                val updated = userService.adminUpdate(id, dto)
                    ?: return@patch call.respond(HttpStatusCode.NotFound)

                call.respond(HttpStatusCode.OK, updated.toResponse())
            }

            // DELETE USER
            delete("/{id}") {
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
            }
        }
    }
}

/*
private suspend fun ApplicationCall.toAdmin(): Admin {
    val principal = principal<JwtPrincipal>()!!
    val user = UserService.read(principal.userId)
        ?: throw ServiceException.NotFound("User not found")
    return Admin(user)
}
*/