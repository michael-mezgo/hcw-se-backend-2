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
