package at.ac.hcw.service

import at.ac.hcw.database.DatabaseUser
import at.ac.hcw.dto.*
import at.ac.hcw.exceptions.UnauthorizedException
import at.ac.hcw.exceptions.UserExistsException
import at.ac.hcw.repository.UserRepository
import at.ac.hcw.security.generateToken
import org.mindrot.jbcrypt.BCrypt

class AuthService(
    private val userRepository: UserRepository
) {
    private fun hashPassword(password: String): String = BCrypt.hashpw(password, BCrypt.gensalt())

    private fun checkPassword(password: String, hash: String): Boolean = BCrypt.checkpw(password, hash)

    fun register(dto: UserRegistration): DatabaseUser {

        val existsByUsername = userRepository.findByUsername(dto.username)
        if (existsByUsername != null) {
            throw UserExistsException(dto.username)
        }

        val existsByEmail = userRepository.findByEmail(dto.email)
        if (existsByEmail != null) {
            throw UserExistsException(dto.email)
        }

        val user = DatabaseUser(
            username = dto.username,
            email = dto.email,
            passwordHash = hashPassword(dto.password),
            firstName = dto.firstName,
            lastName = dto.lastName,
            licenseNumber = dto.licenseNumber,
            licenseValidUntil = dto.licenseValidUntil
        )

        userRepository.save(user)
        return user
    }

    fun login(dto: UserLoginRequest): LoginResponse {

        val user = userRepository.findByUsername(dto.username)
            ?: throw UnauthorizedException(dto.username)

        if (!checkPassword(dto.password, user.passwordHash)) {
            throw UnauthorizedException(dto.username)
        }

        val token = generateToken(user)

        return LoginResponse(
            userId = user.id.toHexString(),
            isAdmin = user.isAdmin,
            token = token
        )
    }
}