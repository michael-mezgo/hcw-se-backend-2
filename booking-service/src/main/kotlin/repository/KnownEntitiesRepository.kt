package at.ac.hcw.repository

import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.InsertOneResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.Document

class KnownEntitiesRepository(database: MongoDatabase) {
    private val usersCollection = database.getCollection("known_users")
    private val carsCollection = database.getCollection("known_cars")

    suspend fun insertUser(userId: String): InsertOneResult = withContext(Dispatchers.IO) {
        usersCollection.insertOne(Document("userId", userId))
    }

    suspend fun deleteUser(userId: String): DeleteResult = withContext(Dispatchers.IO) {
        usersCollection.deleteOne(Filters.eq("userId", userId))
    }

    suspend fun findUser(userId: String): Boolean = withContext(Dispatchers.IO) {
        usersCollection.find(Filters.eq("userId", userId)).first() != null
    }

    suspend fun insertCar(carId: String): InsertOneResult = withContext(Dispatchers.IO) {
        carsCollection.insertOne(Document("carId", carId))
    }

    suspend fun deleteCar(carId: String): DeleteResult = withContext(Dispatchers.IO) {
        carsCollection.deleteOne(Filters.eq("carId", carId))
    }

    suspend fun findCar(carId: String): Boolean = withContext(Dispatchers.IO) {
        carsCollection.find(Filters.eq("carId", carId)).first() != null
    }
}
