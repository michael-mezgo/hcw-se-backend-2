package at.ac.hcw.repository

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import kotlinx.coroutines.Dispatchers
import at.ac.hcw.domain.Car
import at.ac.hcw.domain.FuelType
import at.ac.hcw.domain.Transmission
import at.ac.hcw.dto.CarPatchRequest
import at.ac.hcw.dto.Location
import com.mongodb.client.model.Filters
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import kotlinx.coroutines.withContext
import org.bson.Document
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory

class MongoCarRepository (
    database: MongoDatabase
) : CarRepository {
    private val logger = LoggerFactory.getLogger(MongoCarRepository::class.java)
    private val collection: MongoCollection<Document> = database.getCollection("cars")

    override suspend fun findAll(): List<Car> = withContext(Dispatchers.IO) {
        collection.find()
            .map { it.toCar() }
            .toList()
    }

    override suspend fun findById(id: String): Car? = withContext<Car?>(Dispatchers.IO) {
        if (!ObjectId.isValid(id)) return@withContext null

        collection.find(Filters.eq("_id", ObjectId(id)))
            .first()
            ?.toCar()
    }

    override suspend fun create(car: Car): String = withContext(Dispatchers.IO) {
        val document = car.toDocument()
        collection.insertOne(document)
        document.getObjectId("_id").toHexString()
    }

    override suspend fun patch(id: String, patch: CarPatchRequest): Car? = withContext<Car?>(Dispatchers.IO) {
        if (!ObjectId.isValid(id)) return@withContext null

        val updateDocument = patch.toUpdateDocument()

        if (updateDocument.isEmpty()) return@withContext null

        collection.findOneAndUpdate(
            Filters.eq("_id", ObjectId(id)),
            Document("\$set", updateDocument),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        )?.toCar()
    }

    override suspend fun delete(id: String): Boolean = withContext(Dispatchers.IO) {
        if (!ObjectId.isValid(id)) return@withContext false

        val deletedDocument = collection.findOneAndDelete(
            Filters.eq("_id", ObjectId(id))
        )

        deletedDocument != null
    }

    override suspend fun setAvailability(id: String, availability: Boolean): Boolean = withContext(Dispatchers.IO){
        if (!ObjectId.isValid(id)) return@withContext false

        val result = collection.updateOne(
            Filters.eq("_id", ObjectId(id)),
            Document("\$set", Document("available", availability))
        )
        result.matchedCount > 0
    }

    override suspend fun findAllAvailable(): List<Car> = withContext(Dispatchers.IO){
        collection.find()
            .filter { it.getBoolean("available") == true }
            .map { it.toCar() }
            .toList()
    }

    private fun Car.toDocument(): Document =
        Document()
            .append("manufacturer", manufacturer)
            .append("model", model)
            .append("year", year)
            .append("pricePerDay", pricePerDay)
            .append("description", description)
            .append("imageName", imageName)
            .append("transmission", transmission.name)
            .append("power", power)
            .append("fuelType", fuelType.name)
            .append("available", available)
            .append("location", Document("latitude", location.latitude).append("longitude", location.longitude))

    private fun Document.toCar(): Car {
        val locationDoc = get("location") as? Document
        return Car(
            id = getObjectId("_id")?.toHexString(),
            manufacturer = getString("manufacturer") ?: "",
            model = getString("model") ?: "",
            year = (get("year") as? Number)?.toInt() ?: 0,
            pricePerDay = (get("pricePerDay") as? Number)?.toDouble() ?: 0.0,
            description = getString("description") ?: "",
            imageName = getString("imageName") ?: "",
            transmission = getString("transmission")?.let { raw ->
                runCatching { Transmission.valueOf(raw) }.getOrElse {
                    logger.warn("Invalid transmission value '$raw' in database, defaulting to ${Transmission.AUTOMATIC}")
                    Transmission.AUTOMATIC
                }
            } ?: Transmission.AUTOMATIC,
            power = (get("power") as? Number)?.toInt() ?: 0,
            fuelType = getString("fuelType")?.let { raw ->
                runCatching { FuelType.valueOf(raw) }.getOrElse {
                    logger.warn("Invalid fuelType value '$raw' in database, defaulting to ${FuelType.GASOLINE}")
                    FuelType.GASOLINE
                }
            } ?: FuelType.GASOLINE,
            available = getBoolean("available") ?: false,
            location = Location(
                latitude = (locationDoc?.get("latitude") as? Number)?.toDouble() ?: 0.0,
                longitude = (locationDoc?.get("longitude") as? Number)?.toDouble() ?: 0.0
            )
        )
    }

    private fun CarPatchRequest.toUpdateDocument(): Document {
        val document = Document()

        manufacturer?.let { document["manufacturer"] = it }
        model?.let { document["model"] = it }
        year?.let { document["year"] = it }
        pricePerDayInUSD?.let { document["pricePerDay"] = it }
        description?.let { document["description"] = it }
        transmission?.let { document["transmission"] = it }
        power?.let { document["power"] = it }
        fuelType?.let { document["fuelType"] = it.name }
        location?.let { document["location"] = Document("latitude", it.latitude).append("longitude", it.longitude) }

        return document
    }
}