package at.ac.hcw.repository

import at.ac.hcw.database.DatabaseUser
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.eq
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MongoUserRepository(
    database: MongoDatabase
) : UserRepository {

    private val collection: MongoCollection<DatabaseUser> =
        database.getCollection("users", DatabaseUser::class.java)

    override suspend fun findAll(): List<DatabaseUser> {
        return withContext(Dispatchers.IO) {
            return@withContext collection.find().into(mutableListOf())
        }
    }

    override suspend fun findById(id: String): DatabaseUser? {
        return withContext(Dispatchers.IO) {
            return@withContext collection.find(eq("_id", id)).firstOrNull()
        }

    }

    override suspend fun findByUsername(username: String): DatabaseUser? {
        return withContext(Dispatchers.IO) {
            return@withContext collection.find(eq("username", username)).firstOrNull()
        }
    }

    override suspend fun findByEmail(email: String): DatabaseUser? {
        return withContext(Dispatchers.IO) {
            return@withContext collection.find(eq("email", email)).firstOrNull()
        }
    }

    override suspend fun create(user: DatabaseUser): String {
        withContext(Dispatchers.IO) {
            return@withContext collection.insertOne(user)
        }
        return user.id
    }

    override suspend fun update(id: String, user: DatabaseUser): Boolean {
        return withContext(Dispatchers.IO) {
            val result = collection.replaceOne(eq("_id", id), user)
            return@withContext result.modifiedCount > 0
        }
    }

    override suspend fun delete(id: String): Boolean {
        return withContext(Dispatchers.IO) {
            val result = collection.deleteOne(eq("_id", id))
            return@withContext result.deletedCount > 0
        }
    }
}