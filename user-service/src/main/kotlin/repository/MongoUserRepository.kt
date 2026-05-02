package at.ac.hcw.repository

import at.ac.hcw.database.DatabaseUser
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.eq

class MongoUserRepository(
    database: MongoDatabase
) : UserRepository {

    private val collection: MongoCollection<DatabaseUser> =
        database.getCollection("users", DatabaseUser::class.java)

    override suspend fun findAll(): List<DatabaseUser> {
        return collection.find().into(mutableListOf())
    }

    override suspend fun findById(id: String): DatabaseUser? {
        return collection.find(eq("id", id)).firstOrNull()
    }

    override suspend fun findByUsername(username: String): DatabaseUser? {
        return collection.find(eq("username", username)).firstOrNull()
    }

    override suspend fun findByEmail(email: String): DatabaseUser? {
        return collection.find(eq("email", email)).firstOrNull()
    }

    override suspend fun create(user: DatabaseUser): String {
        collection.insertOne(user)
        return user.id
    }

    override suspend fun update(id: String, user: DatabaseUser): Boolean {
        val result = collection.replaceOne(eq("id", id), user)
        return result.modifiedCount > 0
    }

    override suspend fun delete(id: String): Boolean {
        val result = collection.deleteOne(eq("id", id))
        return result.deletedCount > 0
    }
}