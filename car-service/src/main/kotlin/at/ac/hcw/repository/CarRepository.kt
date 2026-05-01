package at.ac.hcw.repository

import at.ac.hcw.domain.Car
import at.ac.hcw.dto.CarPatchRequest

interface CarRepository {
    suspend fun findAll(): List<Car>
    suspend fun findById(id: String): Car?
    suspend fun create(car: Car): String
    suspend fun patch(id: String, patch: CarPatchRequest): Car?
    suspend fun delete(id: String): Boolean
    suspend fun setAvailability(id: String, availability: Boolean): Boolean
    suspend fun findAllAvailable(): List<Car>
}