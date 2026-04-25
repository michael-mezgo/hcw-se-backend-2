package at.ac.hcw.repository.mongo

import at.ac.hcw.repository.KnownEntitiesRepositoryInterface
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.Document

class MongoKnownEntitiesRepository(database: MongoDatabase) : KnownEntitiesRepositoryInterface {
    private val usersCollection = database.getCollection("known_users")
    private val carsCollection = database.getCollection("known_cars")

    override suspend fun insertUser(userId: String) = withContext(Dispatchers.IO) {
        usersCollection.insertOne(Document("userId", userId))
        Unit
    }

    override suspend fun deleteUser(userId: String) = withContext(Dispatchers.IO) {
        usersCollection.deleteOne(Filters.eq("userId", userId))
        Unit
    }

    override suspend fun findUser(userId: String): Boolean = withContext(Dispatchers.IO) {
        usersCollection.find(Filters.eq("userId", userId)).first() != null
    }

    override suspend fun insertCar(carId: String) = withContext(Dispatchers.IO) {
        carsCollection.insertOne(Document("carId", carId))
        Unit
    }

    override suspend fun deleteCar(carId: String) = withContext(Dispatchers.IO) {
        carsCollection.deleteOne(Filters.eq("carId", carId))
        Unit
    }

    override suspend fun findCar(carId: String): Boolean = withContext(Dispatchers.IO) {
        carsCollection.find(Filters.eq("carId", carId)).first() != null
    }
}