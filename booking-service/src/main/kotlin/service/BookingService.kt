package at.ac.hcw.service

import at.ac.hcw.dto.*
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

    suspend fun cancel(id: String): Booking? =
        repository.deleteById(id)

    suspend fun findByUser(userId: String): List<Booking> =
        repository.findByUserId(userId)
}
