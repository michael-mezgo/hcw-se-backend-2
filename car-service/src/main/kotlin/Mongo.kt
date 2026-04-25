package at.ac.hcw

import com.mongodb.client.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.config.tryGetString
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import at.ac.hcw.Car.CarPatch

fun Application.configureMongo() {
    // Connect to your mongo instance
    val mongoDatabase = connectToMongoDB()
    val carService = runCatching {
        CarService(mongoDatabase)
    }.getOrNull() ?: return

    routing {
        // Get all cars
        get("/cars") {
            val cars = carService.getAllCars()
            call.respond(HttpStatusCode.OK, cars)
        }
        // Get single car
        get("/cars/{id}") {
            val id = call.parameters["id"]
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Missing car id")
                    return@get
                }
            carService.getCar(id)?.let { car ->
                call.respond(HttpStatusCode.OK, car)
            } ?: call.respond(HttpStatusCode.NotFound, "No car with id $id found")
        }
        // Create car
        post("/cars") {
            val car = call.receive<Car>()
            val id = carService.createCar(car)

            call.respond(HttpStatusCode.Created, mapOf("id" to id))
        }
        // Update car
        patch("/cars/{id}") {
            val id = call.parameters["id"]
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing car id")
                return@patch
            }
            val patch = call.receive<CarPatch>()
            val updatedCar = carService.patchCar(id, patch)
            updatedCar?.let {
                call.respond(HttpStatusCode.OK, updatedCar)
            } ?: call.respond(HttpStatusCode.NotFound, "No car with id $id found")
        }
        // Delete car
        delete("/cars/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("No ID found")
            carService.deleteCar(id)?.let {
                call.respond(HttpStatusCode.OK)
            } ?: call.respond(HttpStatusCode.NotFound)
        }
    }
}


/**
 * Establishes connection with a MongoDB database.
 *
 * The following configuration properties (in application.yaml/application.conf) can be specified:
 * * `db.mongo.user` username for your database
 * * `db.mongo.password` password for the user
 * * `db.mongo.host` host that will be used for the database connection
 * * `db.mongo.port` port that will be used for the database connection
 * * `db.mongo.maxPoolSize` maximum number of connections to a MongoDB server
 * * `db.mongo.database.name` name of the database
 *
 * IMPORTANT NOTE: in order to make MongoDB connection working, you have to start a MongoDB server first.
 * See the instructions here: https://www.mongodb.com/docs/manual/administration/install-community/
 * all the paramaters above
 *
 * @returns [MongoDatabase] instance
 * */
fun Application.connectToMongoDB(): MongoDatabase {
    val user = environment.config.tryGetString("db.mongo.user")
    val password = environment.config.tryGetString("db.mongo.password")
    val host = environment.config.tryGetString("db.mongo.host") ?: "127.0.0.1"
    val port = environment.config.tryGetString("db.mongo.port") ?: "27017"
    val maxPoolSize = environment.config.tryGetString("db.mongo.maxPoolSize")?.toInt() ?: 20
    val databaseName = environment.config.tryGetString("db.mongo.database.name") ?: "myDatabase"

    val credentials = user?.let { userVal -> password?.let { passwordVal -> "$userVal:$passwordVal@" } }.orEmpty()
    val uri = "mongodb://armin:$password@ac-fctn9e8-shard-00-00.hnz4nxt.mongodb.net:27017,ac-fctn9e8-shard-00-01.hnz4nxt.mongodb.net:27017,ac-fctn9e8-shard-00-02.hnz4nxt.mongodb.net:27017/?ssl=true&replicaSet=atlas-u70onl-shard-0&authSource=admin&appName=Cluster0"
//    val uri = "mongodb://$credentials$host:$port/?maxPoolSize=$maxPoolSize&w=majority"

    val mongoClient = MongoClients.create(uri)
    val database = mongoClient.getDatabase(databaseName)

    monitor.subscribe(ApplicationStopped) {
        mongoClient.close()
    }

    return database
}
