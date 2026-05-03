package at.ac.hcw

import at.ac.hcw.repository.KnownEntitiesRepositoryInterface
import at.ac.hcw.service.KnownEntitiesService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KnownEntitiesServiceTest {

    private val repository = mockk<KnownEntitiesRepositoryInterface>(relaxUnitFun = true)
    private val service = KnownEntitiesService(repository)

    // --- addUser() ---

    @Test
    fun `addUser - inserts user when not yet known`() = runTest {
        coEvery { repository.findUser("u1") } returns false

        service.addUser("u1")

        coVerify(exactly = 1) { repository.insertUser("u1") }
    }

    @Test
    fun `addUser - skips insert when user already known`() = runTest {
        coEvery { repository.findUser("u1") } returns true

        service.addUser("u1")

        coVerify(exactly = 0) { repository.insertUser(any()) }
    }

    // --- removeUser() ---

    @Test
    fun `removeUser - delegates to repository`() = runTest {
        service.removeUser("u1")

        coVerify(exactly = 1) { repository.deleteUser("u1") }
    }

    // --- userExists() ---

    @Test
    fun `userExists - returns true when user is known`() = runTest {
        coEvery { repository.findUser("u1") } returns true

        assertTrue(service.userExists("u1"))
    }

    @Test
    fun `userExists - returns false when user is unknown`() = runTest {
        coEvery { repository.findUser("u1") } returns false

        assertFalse(service.userExists("u1"))
    }

    // --- addCar() ---

    @Test
    fun `addCar - inserts car when not yet known`() = runTest {
        coEvery { repository.findCar("c1") } returns false

        service.addCar("c1")

        coVerify(exactly = 1) { repository.insertCar("c1") }
    }

    @Test
    fun `addCar - skips insert when car already known`() = runTest {
        coEvery { repository.findCar("c1") } returns true

        service.addCar("c1")

        coVerify(exactly = 0) { repository.insertCar(any()) }
    }

    // --- removeCar() ---

    @Test
    fun `removeCar - delegates to repository`() = runTest {
        service.removeCar("c1")

        coVerify(exactly = 1) { repository.deleteCar("c1") }
    }

    // --- carExists() ---

    @Test
    fun `carExists - returns true when car is known`() = runTest {
        coEvery { repository.findCar("c1") } returns true

        assertTrue(service.carExists("c1"))
    }

    @Test
    fun `carExists - returns false when car is unknown`() = runTest {
        coEvery { repository.findCar("c1") } returns false

        assertFalse(service.carExists("c1"))
    }
}
