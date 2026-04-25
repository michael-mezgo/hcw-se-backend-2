package at.ac.hcw.service

import at.ac.hcw.dto.*
import at.ac.hcw.repository.BookingRepository

class BookingService(
    private val repository: BookingRepository,
    private val knownEntitiesService: KnownEntitiesService
) {

    suspend fun create(request: BookingRequest): BookingResponse {
        if (!knownEntitiesService.userExists(request.userId))
            throw UserNotFoundException(request.userId)
        if (!knownEntitiesService.carExists(request.carId))
            throw CarNotFoundException(request.carId)
        if (repository.findByCarId(request.carId) != null)
            throw CarAlreadyBookedException(request.carId)

        val id = repository.insert(request)
        return BookingResponse(id, request.userId, request.carId)
    }

    suspend fun cancel(id: String): BookingResponse? =
        repository.deleteById(id)

    suspend fun findByUser(userId: String): List<BookingResponse> =
        repository.findByUserId(userId)
}
