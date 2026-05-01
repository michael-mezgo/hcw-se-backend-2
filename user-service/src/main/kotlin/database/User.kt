package at.ac.hcw.database

import at.ac.hcw.dto.UserEvent
import at.ac.hcw.dto.UserResponse
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class DatabaseUser(
    @BsonId
    val id: ObjectId = ObjectId(),

    val username: String,
    val email: String,
    val passwordHash: String,
    val firstName: String,
    val lastName: String,
    val licenseNumber: String,
    val licenseValidUntil: String,
    val isAdmin: Boolean = false
)

fun DatabaseUser.toEvent() = UserEvent(id.toHexString())

fun DatabaseUser.toResponse() = UserResponse(
    id = id.toHexString(),
    username = username,
    email = email,
    firstName = firstName,
    lastName = lastName,
    licenseNumber = licenseNumber,
    licenseValidUntil = licenseValidUntil,
    isAdmin = isAdmin,
)