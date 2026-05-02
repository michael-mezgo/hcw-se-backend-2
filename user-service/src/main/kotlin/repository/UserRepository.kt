package at.ac.hcw.repository

import at.ac.hcw.database.DatabaseUser

interface UserRepository {
    suspend fun findAll(): List<DatabaseUser>
    suspend fun findById(id: String): DatabaseUser?
    suspend fun findByUsername(username: String): DatabaseUser?
    suspend fun findByEmail(email: String): DatabaseUser?
    suspend fun create(user: DatabaseUser): String?
    suspend fun update(id: String, user: DatabaseUser): Boolean
    suspend fun delete(id: String): Boolean
}