package at.ac.hcw

import at.ac.hcw.database.DatabaseUser
import at.ac.hcw.dto.UserLoginRequest
import at.ac.hcw.dto.UserRegistration
import at.ac.hcw.exceptions.UnauthorizedException
import at.ac.hcw.exceptions.UserExistsException
import at.ac.hcw.repository.UserRepository
import at.ac.hcw.security.JwtConfig
import at.ac.hcw.service.AuthService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.mindrot.jbcrypt.BCrypt
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuthServiceTest {

    private val repository = mockk<UserRepository>()
    private val service = AuthService(repository)

    private val registration = UserRegistration(
        username = "alice",
        email = "alice@example.com",
        password = "secret",
        firstName = "Alice",
        lastName = "Adams",
        licenseNumber = "L-1",
        preferredCurrency = "USD",
        licenseValidUntil = "2030-12-31"
    )

    @BeforeTest
    fun setupJwtConfig() {
        JwtConfig.secret = "test-secret"
        JwtConfig.issuer = "test-issuer"
        JwtConfig.audience = "test-audience"
    }

    // --- register() ---

    @Test
    fun `register - success creates user with hashed password`() = runTest {
        coEvery { repository.findByUsername("alice") } returns null
        coEvery { repository.findByEmail("alice@example.com") } returns null
        coEvery { repository.create(any()) } returns "new-id"

        val result = service.register(registration)

        assertEquals("alice", result.username)
        assertEquals("alice@example.com", result.email)
        assertTrue(BCrypt.checkpw("secret", result.passwordHash))
        coVerify(exactly = 1) { repository.create(any()) }
    }

    @Test
    fun `register - throws UserExistsException when username exists`() = runTest {
        coEvery { repository.findByUsername("alice") } returns existingUser("alice", "other@x.com")

        assertFailsWith<UserExistsException> { service.register(registration) }
        coVerify(exactly = 0) { repository.create(any()) }
    }

    @Test
    fun `register - throws UserExistsException when email exists`() = runTest {
        coEvery { repository.findByUsername("alice") } returns null
        coEvery { repository.findByEmail("alice@example.com") } returns existingUser("other", "alice@example.com")

        assertFailsWith<UserExistsException> { service.register(registration) }
        coVerify(exactly = 0) { repository.create(any()) }
    }

    // --- login() ---

    @Test
    fun `login - success returns token and user info`() = runTest {
        val user = existingUser("alice", "alice@example.com", password = "secret", isAdmin = true)
        coEvery { repository.findByUsername("alice") } returns user

        val response = service.login(UserLoginRequest("alice", "secret"))

        assertEquals(user.id, response.userId)
        assertTrue(response.isAdmin)
        assertNotNull(response.token)
        assertTrue(response.token.isNotBlank())
    }

    @Test
    fun `login - throws UnauthorizedException when user not found`() = runTest {
        coEvery { repository.findByUsername("alice") } returns null

        assertFailsWith<UnauthorizedException> {
            service.login(UserLoginRequest("alice", "secret"))
        }
    }

    @Test
    fun `login - throws UnauthorizedException when password incorrect`() = runTest {
        val user = existingUser("alice", "alice@example.com", password = "secret")
        coEvery { repository.findByUsername("alice") } returns user

        assertFailsWith<UnauthorizedException> {
            service.login(UserLoginRequest("alice", "wrong"))
        }
    }

    private fun existingUser(
        username: String,
        email: String,
        password: String = "secret",
        isAdmin: Boolean = false
    ) = DatabaseUser(
        username = username,
        email = email,
        passwordHash = BCrypt.hashpw(password, BCrypt.gensalt()),
        firstName = "First",
        lastName = "Last",
        licenseNumber = "L-1",
        licenseValidUntil = "2030-12-31",
        isAdmin = isAdmin
    )
}
