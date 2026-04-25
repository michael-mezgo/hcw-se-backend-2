package at.ac.hcw.model

import at.ac.hcw.dto.BookingCancelledEvent
import at.ac.hcw.dto.BookingCreatedEvent
import at.ac.hcw.dto.BookingResponse

data class Booking(
    val id: String,
    val userId: String,
    val carId: String
)

fun Booking.toResponse() = BookingResponse(id = id, userId = userId, carId = carId)
fun Booking.toCreatedEvent() = BookingCreatedEvent(bookingId = id, userId = userId, carId = carId)
fun Booking.toCancelledEvent() = BookingCancelledEvent(bookingId = id, carId = carId)
