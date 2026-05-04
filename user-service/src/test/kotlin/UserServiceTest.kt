package at.ac.hcw

import at.ac.hcw.database.DatabaseUser
import at.ac.hcw.dto.AdminUserCreate
import at.ac.hcw.dto.AdminUserUpdate
import at.ac.hcw.dto.UserUpdate
import at.ac.hcw.repository.UserRepository
import at.ac.hcw.service.UserService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.mindrot.jbcrypt.BCrypt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserServiceTest {

    private val repository = mockk<UserRepository>()
    private val service = UserService(repository)

    private fun existingUser(id: String = "u1") = DatabaseUser(
        id = id,
        username = "alice",
        email = "alice@example.com",
        passwordHash = BCrypt.hashpw("oldpass", BCrypt.gensalt()),
        firstName = "Alice",
        lastName = "Adams",
        licenseNumber = "L-1",
        licenseValidUntil = "2030-12-31",
        isAdmin = false,
        preferredCurrency = "USD"
    )

    // --- findById() ---

    @Test
    fun `findById - returns user when found`() = runTest {
        val user = existingUser()
        coEvery { repository.findById("u1") } returns user

        val result = service.findById("u1")

        assertEquals(user, result)
    }

    @Test
    fun `findById - returns null when not found`() = runTest {
        coEvery { repository.findById("unknown") } returns null

        assertNull(service.findById("unknown"))
    }

    // --- findAll() ---

    @Test
    fun `findAll - returns all users`() = runTest {
        val users = listOf(existingUser("u1"), existingUser("u2"))
        coEvery { repository.findAll() } returns users

        val result = service.findAll()

        assertEquals(users, result)
    }

    // --- update() ---

    @Test
    fun `update - returns null when user not found`() = runTest {
        coEvery { repository.findById("unknown") } returns null

        val result = service.update("unknown", UserUpdate(firstName = "Bob"))

        assertNull(result)
        coVerify(exactly = 0) { repository.update(any(), any()) }
    }

    @Test
    fun `update - applies only provided fields and keeps the rest`() = runTest {
        val user = existingUser()
        coEvery { repository.findById("u1") } returns user
        coEvery { repository.update("u1", any()) } returns true

        val result = service.update("u1", UserUpdate(firstName = "Bob"))

        assertEquals("Bob", result?.firstName)
        assertEquals(user.lastName, result?.lastName)
        assertEquals(user.email, result?.email)
        assertEquals(user.passwordHash, result?.passwordHash)
    }

    @Test
    fun `update - hashes new password when provided`() = runTest {
        val user = existingUser()
        coEvery { repository.findById("u1") } returns user
        val captured = slot<DatabaseUser>()
        coEvery { repository.update("u1", capture(captured)) } returns true

        val result = service.update("u1", UserUpdate(password = "newpass"))

        assertNotEquals(user.passwordHash, result?.passwordHash)
        assertTrue(BCrypt.checkpw("newpass", captured.captured.passwordHash))
    }

    @Test
    fun `update - returns null when repository update fails`() = runTest {
        coEvery { repository.findById("u1") } returns existingUser()
        coEvery { repository.update("u1", any()) } returns false

        val result = service.update("u1", UserUpdate(firstName = "Bob"))

        assertNull(result)
    }

    // --- delete() ---

    @Test
    fun `delete - returns deleted user when successful`() = runTest {
        val user = existingUser()
        coEvery { repository.findById("u1") } returns user
        coEvery { repository.delete("u1") } returns true

        val result = service.delete("u1")

        assertEquals(user, result)
    }

    @Test
    fun `delete - returns null when user not found`() = runTest {
        coEvery { repository.findById("unknown") } returns null

        val result = service.delete("unknown")

        assertNull(result)
        coVerify(exactly = 0) { repository.delete(any()) }
    }

    @Test
    fun `delete - returns null when repository delete fails`() = runTest {
        coEvery { repository.findById("u1") } returns existingUser()
        coEvery { repository.delete("u1") } returns false

        val result = service.delete("u1")

        assertNull(result)
    }

    // --- adminCreate() ---

    @Test
    fun `adminCreate - returns user when repository returns id`() = runTest {
        val dto = AdminUserCreate(
            username = "alice",
            email = "alice@example.com",
            password = "secret",
            firstName = "Alice",
            lastName = "Adams",
            licenseNumber = "L-1",
            licenseValidUntil = "2030-12-31",
            isAdmin = true
        )
        coEvery { repository.create(any()) } returns "new-id"

        val result = service.adminCreate(dto)

        assertEquals("alice", result.username)
        assertEquals("alice@example.com", result.email)
        assertTrue(result.isAdmin)
        assertTrue(BCrypt.checkpw("secret", result.passwordHash))
    }

    @Test
    fun `adminCreate - throws when repository returns null`() = runTest {
        val dto = AdminUserCreate(
            username = "alice",
            email = "alice@example.com",
            password = "secret",
            firstName = "Alice",
            lastName = "Adams",
            licenseNumber = "L-1",
            licenseValidUntil = "2030-12-31"
        )
        coEvery { repository.create(any()) } returns null

        assertFailsWith<Exception> { service.adminCreate(dto) }
    }

    @Test
    fun `adminCreate - throws when repository returns blank id`() = runTest {
        val dto = AdminUserCreate(
            username = "alice",
            email = "alice@example.com",
            password = "secret",
            firstName = "Alice",
            lastName = "Adams",
            licenseNumber = "L-1",
            licenseValidUntil = "2030-12-31"
        )
        coEvery { repository.create(any()) } returns ""

        assertFailsWith<Exception> { service.adminCreate(dto) }
    }

    // --- adminUpdate() ---

    @Test
    fun `adminUpdate - returns null when user not found`() = runTest {
        coEvery { repository.findById("unknown") } returns null

        val result = service.adminUpdate("unknown", AdminUserUpdate(email = "new@x.com"))

        assertNull(result)
        coVerify(exactly = 0) { repository.update(any(), any()) }
    }

    @Test
    fun `adminUpdate - applies provided fields including isAdmin`() = runTest {
        val user = existingUser()
        coEvery { repository.findById("u1") } returns user
        coEvery { repository.update("u1", any()) } returns true

        val result = service.adminUpdate("u1", AdminUserUpdate(email = "new@x.com", isAdmin = true))

        assertEquals("new@x.com", result?.email)
        assertTrue(result?.isAdmin == true)
        assertEquals(user.firstName, result.firstName)
    }

    @Test
    fun `adminUpdate - returns null when repository update fails`() = runTest {
        coEvery { repository.findById("u1") } returns existingUser()
        coEvery { repository.update("u1", any()) } returns false

        val result = service.adminUpdate("u1", AdminUserUpdate(email = "new@x.com"))

        assertNull(result)
    }
}
