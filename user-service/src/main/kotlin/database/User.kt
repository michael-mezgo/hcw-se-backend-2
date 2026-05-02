package at.ac.hcw.database

import at.ac.hcw.dto.UserEvent
import at.ac.hcw.dto.UserResponse
import org.bson.codecs.pojo.annotations.BsonId

data class DatabaseUser(
    @BsonId var id: String = java.util.UUID.randomUUID().toString(),
    var username: String = "",
    var email: String = "",
    var passwordHash: String = "",
    var firstName: String = "",
    var lastName: String = "",
    var licenseNumber: String = "",
    var licenseValidUntil: String = "",
    var isAdmin: Boolean = false,
    var preferredCurrency: String = ""
)

fun DatabaseUser.toEvent() = UserEvent(id)

fun DatabaseUser.toResponse() = UserResponse(
    id = id,
    username = username,
    email = email,
    firstName = firstName,
    lastName = lastName,
    licenseNumber = licenseNumber,
    licenseValidUntil = licenseValidUntil,
    isAdmin = isAdmin,
    preferredCurrency = preferredCurrency,
)