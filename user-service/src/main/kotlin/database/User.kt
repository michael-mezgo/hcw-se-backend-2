package at.ac.hcw.database

import at.ac.hcw.dto.UserEvent
import at.ac.hcw.dto.UserResponse

data class DatabaseUser(
    val id: String = java.util.UUID.randomUUID().toString(),
    val username: String = "",
    val email: String = "",
    val passwordHash: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val licenseNumber: String = "",
    val licenseValidUntil: String = "",
    val isAdmin: Boolean = false
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
)