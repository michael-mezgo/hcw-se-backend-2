package at.ac.hcw.dto

import kotlinx.serialization.Serializable

@Serializable
data class BookingCreatedEvent(
    val bookingId: String,
    val carId: String
)

@Serializable
data class BookingCancelledEvent(
    val bookingId: String,
    val carId: String
)