package at.ac.hcw.service

import at.ac.hcw.domain.Car
import at.ac.hcw.dto.CarPatchRequest
import at.ac.hcw.repository.CarRepository

class CarService (
    private val carRepository: CarRepository
) {
    suspend fun getAllCars(): List<Car> =
        carRepository.findAll()

    suspend fun getCar(id: String): Car? =
        carRepository.findById(id)

    suspend fun createCar(car: Car): String =
        carRepository.create(car)

    suspend fun patchCar(id: String, patch: CarPatchRequest): Car? =
        carRepository.patch(id, patch)

    suspend fun deleteCar(id: String): Boolean =
        carRepository.delete(id)

    suspend fun markCarAsBooked(carId: String): Boolean =
        carRepository.setAvailability(carId, false)

    suspend fun markCarAsAvailable(carId: String): Boolean =
        carRepository.setAvailability(carId, true)
}