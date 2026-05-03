package at.ac.hcw.service

import at.ac.hcw.exceptions.CarAlreadyBookedException
import at.ac.hcw.exceptions.CarNotFoundException
import at.ac.hcw.exceptions.UserNotFoundException
import at.ac.hcw.dto.*
import at.ac.hcw.exceptions.CarNotBookedException
import at.ac.hcw.model.Booking
import at.ac.hcw.repository.BookingRepositoryInterface

class BookingService(
    private val repository: BookingRepositoryInterface,
    private val knownEntitiesService: KnownEntitiesService
) {

    suspend fun create(request: BookingRequest): Booking {
        if (!knownEntitiesService.userExists(request.userId))
            throw UserNotFoundException(request.userId)
        if (!knownEntitiesService.carExists(request.carId))
            throw CarNotFoundException(request.carId)
        if (repository.findByCarId(request.carId) != null)
            throw CarAlreadyBookedException(request.carId)

        return repository.insert(request.userId, request.carId)
    }

    suspend fun findById(id: String): Booking? =
        repository.findById(id)

    suspend fun cancel(id: String): Booking {
        return repository.deleteById(id) ?: throw CarNotBookedException(id)
    }


    suspend fun findByUser(userId: String): List<Booking> =
        repository.findByUserId(userId)
}
