package at.ac.hcw.service

import at.ac.hcw.business.User
import at.ac.hcw.dto.UserUpdate
import at.ac.hcw.database.DatabaseUser
import at.ac.hcw.dto.AdminUserCreate
import at.ac.hcw.dto.AdminUserUpdate
import at.ac.hcw.repository.UserRepository
import org.bson.types.ObjectId

class UserService(
    private val userRepository: UserRepository
) {

    fun findById(id: String): DatabaseUser? {
        return userRepository.findById(id)
    }

    fun findAll(): List<DatabaseUser> {
        return userRepository.findAll()
    }

    fun update(id: String, update: UserUpdate): DatabaseUser? {
        val existing = userRepository.findById(id) ?: return null

        val updated = existing.copy(
            email = update.email ?: existing.email,
            firstName = update.firstName ?: existing.firstName,
            lastName = update.lastName ?: existing.lastName,
            licenseNumber = update.licenseNumber ?: existing.licenseNumber,
            licenseValidUntil = update.licenseValidUntil ?: existing.licenseValidUntil
        )

        userRepository.update(id, updated)
        return updated
    }

    fun delete(id: String): DatabaseUser? {
        val existing = userRepository.findById(id) ?: return null

        userRepository.deleteById(id)

        return existing
    }

    // ── ADMIN CREATE ──────────────────────────────────────

    fun adminCreate(dto: AdminUserCreate): User {

        val user = User(
            id = ObjectId().toHexString(),
            username = dto.username,
            email = dto.email,
            firstName = dto.firstName,
            lastName = dto.lastName,
            licenseNumber = dto.licenseNumber,
            licenseValidUntil = dto.licenseValidUntil,
            isAdmin = dto.isAdmin,
            isLocked = false
        )

        userRepository.save(user)

        return user
    }

    // ── ADMIN UPDATE ──────────────────────────────────────

    fun adminUpdate(id: String, dto: AdminUserUpdate): DatabaseUser? {

        val existing = userRepository.findById(id) ?: return null

        val updated = existing.copy(
            email = dto.email ?: existing.email,
            firstName = dto.firstName ?: existing.firstName,
            lastName = dto.lastName ?: existing.lastName,
            licenseNumber = dto.licenseNumber ?: existing.licenseNumber,
            licenseValidUntil = dto.licenseValidUntil ?: existing.licenseValidUntil,
            isAdmin = dto.isAdmin ?: existing.isAdmin,
            isLocked = dto.isLocked ?: existing.isLocked
        )

        userRepository.update(id, updated)

        return updated
    }

}