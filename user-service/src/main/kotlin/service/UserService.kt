package at.ac.hcw.service

import at.ac.hcw.dto.UserUpdate
import at.ac.hcw.database.DatabaseUser
import at.ac.hcw.dto.AdminUserCreate
import at.ac.hcw.dto.AdminUserUpdate
import at.ac.hcw.repository.UserRepository
import org.mindrot.jbcrypt.BCrypt

class UserService(
    private val userRepository: UserRepository
) {
    private fun hashPassword(password: String): String =
        BCrypt.hashpw(password, BCrypt.gensalt())

    suspend fun findById(id: String): DatabaseUser? {
        return userRepository.findById(id)
    }

    suspend fun findAll(): List<DatabaseUser> {
        return userRepository.findAll()
    }

    suspend fun update(id: String, update: UserUpdate): DatabaseUser? {
        val existing = userRepository.findById(id) ?: return null

        val updated = existing.copy(
            email = update.email ?: existing.email,
            passwordHash = update.password?.let { hashPassword(it) } ?: existing.passwordHash,
            firstName = update.firstName ?: existing.firstName,
            lastName = update.lastName ?: existing.lastName,
            licenseNumber = update.licenseNumber ?: existing.licenseNumber,
            licenseValidUntil = update.licenseValidUntil ?: existing.licenseValidUntil,
            preferredCurrency = update.preferredCurrency ?: existing.preferredCurrency
        )

        val result = userRepository.update(id, updated)

        if (result) {
            return updated
        } else {
            return null
        }
    }

    suspend fun delete(id: String): DatabaseUser? {
        val existing = userRepository.findById(id) ?: return null

        return if (userRepository.delete(id))
            existing
        else
            null
    }

    // ── ADMIN CREATE ──────────────────────────────────────

    suspend fun adminCreate(dto: AdminUserCreate): DatabaseUser {

        val user = DatabaseUser(
            username = dto.username,
            email = dto.email,
            passwordHash = hashPassword(dto.password),
            firstName = dto.firstName,
            lastName = dto.lastName,
            licenseNumber = dto.licenseNumber,
            licenseValidUntil = dto.licenseValidUntil,
            isAdmin = dto.isAdmin
        )

        if(userRepository.create(user)?.isNotBlank() ?: false)
            return user
        else
            throw Exception("Failed to create user")
    }

    // ── ADMIN UPDATE ──────────────────────────────────────

    suspend fun adminUpdate(id: String, dto: AdminUserUpdate): DatabaseUser? {

        val existing = userRepository.findById(id) ?: return null

        val updated = existing.copy(
            email = dto.email ?: existing.email,
            firstName = dto.firstName ?: existing.firstName,
            passwordHash = dto.password?.let { hashPassword(it) } ?: existing.passwordHash,
            lastName = dto.lastName ?: existing.lastName,
            licenseNumber = dto.licenseNumber ?: existing.licenseNumber,
            licenseValidUntil = dto.licenseValidUntil ?: existing.licenseValidUntil,
            isAdmin = dto.isAdmin ?: existing.isAdmin,
            preferredCurrency = dto.preferredCurrency ?: existing.preferredCurrency
        )

        val result = userRepository.update(id, updated)

        return if (result) updated else null
    }
}