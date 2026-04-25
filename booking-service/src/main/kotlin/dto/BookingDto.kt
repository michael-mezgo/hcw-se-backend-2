package at.ac.hcw.dto

import kotlinx.serialization.Serializable

@Serializable
data class BookingRequest(
    val userId: String,
    val carId: String
)

@Serializable
data class BookingResponse(
    val id: String,
    val userId: String,
    val carId: String
)

@Serializable
data class BookingCreatedEvent(
    val bookingId: String,
    val userId: String,
    val carId: String
)

@Serializable
data class BookingCancelledEvent(
    val bookingId: String,
    val carId: String
)

@Serializable
data class UserEvent(val userId: String)

@Serializable
data class CarEvent(val carId: String)
