package at.ac.hcw.service

import at.ac.hcw.database.DatabaseUser
import at.ac.hcw.dto.*
import at.ac.hcw.exceptions.UnauthorizedException
import at.ac.hcw.exceptions.UserExistsException
import at.ac.hcw.repository.UserRepository
import at.ac.hcw.security.generateToken

class AuthService(
    private val userRepository: UserRepository
) {

    fun register(dto: UserRegistration): DatabaseUser {

        val exists = userRepository.findByUsername(dto.username)
        if (exists != null) {
            throw UserExistsException(dto.username)
        }

        val user = DatabaseUser(
            username = dto.username,
            email = dto.email,
            passwordHash = dto.password,
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

        if (user.passwordHash != dto.password) {
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