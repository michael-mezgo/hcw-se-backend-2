package at.ac.hcw.repository

import at.ac.hcw.model.Booking

interface BookingRepositoryInterface {
    suspend fun insert(userId: String, carId: String): Booking
    suspend fun deleteById(id: String): Booking?
    suspend fun findByUserId(userId: String): List<Booking>
    suspend fun findByCarId(carId: String): Booking?
}
