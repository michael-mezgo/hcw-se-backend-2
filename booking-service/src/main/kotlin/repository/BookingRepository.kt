package at.ac.hcw.repository

import at.ac.hcw.dto.BookingRequest
import at.ac.hcw.dto.BookingResponse
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.Document
import org.bson.types.ObjectId

class BookingRepository(database: MongoDatabase) {
    private val collection = database.getCollection("bookings")

    suspend fun insert(request: BookingRequest): String = withContext(Dispatchers.IO) {
        val doc = Document().apply {
            append("userId", request.userId)
            append("carId", request.carId)
        }
        collection.insertOne(doc)
        doc["_id"].toString()
    }

    suspend fun deleteById(id: String): BookingResponse? = withContext(Dispatchers.IO) {
        collection.findOneAndDelete(Filters.eq("_id", ObjectId(id)))?.toResponse()
    }

    suspend fun findByUserId(userId: String): List<BookingResponse> = withContext(Dispatchers.IO) {
        collection.find(Filters.eq("userId", userId)).map { it.toResponse() }.toList()
    }

    suspend fun findByCarId(carId: String): BookingResponse? = withContext(Dispatchers.IO) {
        collection.find(Filters.eq("carId", carId)).first()?.toResponse()
    }

    private fun Document.toResponse() = BookingResponse(
        id = this["_id"].toString(),
        userId = getString("userId"),
        carId = getString("carId")
    )
}
