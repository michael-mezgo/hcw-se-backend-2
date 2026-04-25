package at.ac.hcw

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.bson.Document
import org.bson.types.ObjectId

@Serializable
data class Car(
    val manufacturer: String,
    val model: String,
    val year: Int,
    val pricePerDay: Double,
    val description: String,
    val imageName: String,
    val transmission: String,
    val power: Int,
    val fuelType: String,
/*    val bookedBy: User?,
    val location: Coordinate*/
) {
    fun toDocument(): Document = Document.parse(Json.encodeToString(this))

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromDocument(document: Document): Car = json.decodeFromString(document.toJson())
    }

    @Serializable
    data class CarPatch(
        val manufacturer: String? = null,
        val model: String? = null,
        val year: Int? = null,
        val pricePerDay: Double? = null,
        val description: String? = null,
        val imageName: String? = null,
        val transmission: String? = null,
        val power: Int? = null,
        val fuelType: String? = null
    ) {
        fun toUpdateDocument(): Document {
            val document = Document()

            manufacturer?.let { document["manufacturer"] = it }
            model?.let { document["model"] = it }
            year?.let { document["year"] = it }
            pricePerDay?.let { document["pricePerDay"] = it }
            description?.let { document["description"] = it }
            imageName?.let { document["imageName"] = it }
            transmission?.let { document["transmission"] = it }
            power?.let { document["power"] = it }
            fuelType?.let { document["fuelType"] = it }

            return document
        }
    }
}

class CarService(private val database: MongoDatabase) {
    var collection: MongoCollection<Document> = database.getCollection("cars")

    // Create new car (for admins)
    suspend fun createCar(car: Car): String = withContext(Dispatchers.IO) {
        val doc = car.toDocument()
        collection.insertOne(doc)
        doc.getObjectId("_id").toHexString()
    }

    // Get all cars
    suspend fun getAllCars(): List<Car> = withContext(Dispatchers.IO) {
        collection.find().map { Car.fromDocument(it) }.toList()
    }

    // Get single car
    suspend fun getCar(id: String): Car? = withContext(Dispatchers.IO) {
        collection.find(Filters.eq("_id", ObjectId(id))).first()?.let(Car::fromDocument)
    }

    // Patch a car (for admins)
    suspend fun patchCar(id: String, car: Car.CarPatch): Car? = withContext<Car?>(Dispatchers.IO) {
        if (!ObjectId.isValid(id)) return@withContext null
        val updateDocument = car.toUpdateDocument()
        if (updateDocument.isEmpty()) return@withContext null
        collection.findOneAndUpdate(
            Filters.eq("_id",
                ObjectId(id)),
            Document("\$set", updateDocument),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        )?.let(Car::fromDocument)
    }

    // Delete a car (for admins)
    suspend fun deleteCar(id: String): Document? = withContext(Dispatchers.IO) {
        collection.findOneAndDelete(Filters.eq("_id", ObjectId(id)))
    }
}
