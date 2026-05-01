package at.ac.hcw.repository.mongo

import at.ac.hcw.model.Booking
import at.ac.hcw.repository.BookingRepositoryInterface
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.Document
import org.bson.types.ObjectId

class MongoBookingRepository(database: MongoDatabase) : BookingRepositoryInterface {
    private val collection = database.getCollection("bookings")

    override suspend fun insert(userId: String, carId: String): Booking = withContext(Dispatchers.IO) {
        val doc = Document().apply {
            append("userId", userId)
            append("carId", carId)
        }
        collection.insertOne(doc)
        Booking(id = doc["_id"].toString(), userId = userId, carId = carId)
    }

    override suspend fun findById(id: String): Booking? = withContext(Dispatchers.IO) {
        if (!ObjectId.isValid(id)) {
            return@withContext null
        }
        collection.find(Filters.eq("_id", ObjectId(id))).first()?.toBooking()
    }

    override suspend fun deleteById(id: String): Booking? = withContext(Dispatchers.IO) {
        if (!ObjectId.isValid(id)) {
            return@withContext null
        }
        collection.findOneAndDelete(Filters.eq("_id", ObjectId(id)))?.toBooking()
    }

    override suspend fun findByUserId(userId: String): List<Booking> = withContext(Dispatchers.IO) {
        collection.find(Filters.eq("userId", userId)).map { it.toBooking() }.toList()
    }

    override suspend fun findByCarId(carId: String): Booking? = withContext(Dispatchers.IO) {
        collection.find(Filters.eq("carId", carId)).first()?.toBooking()
    }

    private fun Document.toBooking() = Booking(
        id = this["_id"].toString(),
        userId = getString("userId"),
        carId = getString("carId")
    )
}