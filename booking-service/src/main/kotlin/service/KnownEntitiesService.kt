package at.ac.hcw.service

import at.ac.hcw.repository.KnownEntitiesRepository

class KnownEntitiesService(private val repository: KnownEntitiesRepository) {

    suspend fun addUser(userId: String) {
        if (!repository.findUser(userId)) repository.insertUser(userId)
    }

    suspend fun removeUser(userId: String) =
        repository.deleteUser(userId)

    suspend fun userExists(userId: String): Boolean =
        repository.findUser(userId)

    suspend fun addCar(carId: String) {
        if (!repository.findCar(carId)) repository.insertCar(carId)
    }

    suspend fun removeCar(carId: String) =
        repository.deleteCar(carId)

    suspend fun carExists(carId: String): Boolean =
        repository.findCar(carId)
}
