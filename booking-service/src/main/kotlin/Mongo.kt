package at.ac.hcw

import at.ac.hcw.repository.mongo.MongoBookingRepository
import at.ac.hcw.repository.mongo.MongoKnownEntitiesRepository
import at.ac.hcw.service.BookingService
import at.ac.hcw.service.KnownEntitiesService
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.util.*

val BookingServiceKey = AttributeKey<BookingService>("BookingService")
val KnownEntitiesServiceKey = AttributeKey<KnownEntitiesService>("KnownEntitiesService")

fun Application.configureMongo() {
    val db = connectToMongoDB()

    val knownEntitiesRepository = MongoKnownEntitiesRepository(db)
    val knownEntitiesService = KnownEntitiesService(knownEntitiesRepository)

    val bookingRepository = MongoBookingRepository(db)
    val bookingService = BookingService(bookingRepository, knownEntitiesService)

    attributes.put(KnownEntitiesServiceKey, knownEntitiesService)
    attributes.put(BookingServiceKey, bookingService)
}

fun Application.connectToMongoDB(): MongoDatabase {
    val user = environment.config.tryGetString("db.mongo.user")
    val password = environment.config.tryGetString("db.mongo.password")
    val host = environment.config.tryGetString("db.mongo.host") ?: "127.0.0.1"
    val port = environment.config.tryGetString("db.mongo.port") ?: "27019"
    val maxPoolSize = environment.config.tryGetString("db.mongo.maxPoolSize")?.toInt() ?: 20
    val databaseName = environment.config.tryGetString("db.mongo.database.name") ?: "booking-service"

    val credentials = user?.let { u -> password?.let { p -> "$u:$p@" } }.orEmpty()
    val uri = "mongodb://$credentials$host:$port/?maxPoolSize=$maxPoolSize&w=majority"

    val mongoClient = MongoClients.create(uri)
    val database = mongoClient.getDatabase(databaseName)

    monitor.subscribe(ApplicationStopped) {
        mongoClient.close()
    }

    return database
}
