package at.ac.hcw.repository

import at.ac.hcw.database.DatabaseUser
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.eq
import org.bson.types.ObjectId

class UserRepository(
    database: MongoDatabase
)
{
    private val collection: MongoCollection<DatabaseUser> = database.getCollection("users", DatabaseUser::class.java)

    fun findById(id: String): DatabaseUser? {
        return collection.find(eq("_id", ObjectId(id))).firstOrNull()
    }

    fun findAll(): List<DatabaseUser> {
        return collection.find().into(mutableListOf())
    }

    fun findByUsername(username: String): DatabaseUser? {
        return collection.find(eq("username", username)).firstOrNull()
    }

    fun save(user: DatabaseUser) {
        collection.insertOne(user)
    }

    fun update(id: String, user: DatabaseUser) {
        collection.replaceOne(
            eq("_id", ObjectId(id)),
            user
        )
    }

    fun deleteById(id: String) {
        collection.deleteOne(eq("_id", ObjectId(id)))
    }
}