package at.ac.hcw

import at.ac.hcw.repository.MongoUserRepository
import at.ac.hcw.service.AuthService
import at.ac.hcw.service.UserService
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.util.*
import org.bson.codecs.configuration.CodecRegistries.*
import org.bson.codecs.pojo.PojoCodecProvider

val UserServiceKey = AttributeKey<UserService>("UserService")
val AuthServiceKey = AttributeKey<AuthService>("AuthService")

fun Application.configureMongo() {
    val database = connectToMongoDB()
    val userRepository = MongoUserRepository(database)

    attributes.put(UserServiceKey, UserService(userRepository))
    attributes.put(AuthServiceKey, AuthService(userRepository))
}

fun Application.connectToMongoDB(): MongoDatabase {
    val user = environment.config.tryGetString("db.mongo.user")
    val password = environment.config.tryGetString("db.mongo.password")
    val host = environment.config.tryGetString("db.mongo.host") ?: "127.0.0.1"
    val port = environment.config.tryGetString("db.mongo.port") ?: "27019"
    val maxPoolSize = environment.config.tryGetString("db.mongo.maxPoolSize")?.toInt() ?: 20
    val databaseName = environment.config.tryGetString("db.mongo.database.name") ?: "user-service"

    val credentials = user?.let { u -> password?.let { p -> "$u:$p@" } }.orEmpty()
    val uri = "mongodb://$credentials$host:$port/?maxPoolSize=$maxPoolSize&w=majority"

    val pojoCodecRegistry = fromProviders(
        PojoCodecProvider.builder().automatic(true).build()
    )

    val codecRegistry = fromRegistries(
        MongoClientSettings.getDefaultCodecRegistry(),
        pojoCodecRegistry
    )

    val settings = MongoClientSettings.builder()
        .applyConnectionString(com.mongodb.ConnectionString(uri))
        .codecRegistry(codecRegistry)
        .build()

    val client = MongoClients.create(settings)
    val database = client.getDatabase(databaseName)

    monitor.subscribe(ApplicationStopped) {
        client.close()
    }

    return database
}