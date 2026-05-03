package at.ac.hcw

import at.ac.hcw.dto.BookingRequest
import at.ac.hcw.exceptions.CarAlreadyBookedException
import at.ac.hcw.exceptions.CarNotFoundException
import at.ac.hcw.exceptions.CarNotBookedException
import at.ac.hcw.exceptions.UserNotFoundException
import at.ac.hcw.model.Booking
import at.ac.hcw.repository.BookingRepositoryInterface
import at.ac.hcw.service.BookingService
import at.ac.hcw.service.KnownEntitiesService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class BookingServiceTest {

    private val repository = mockk<BookingRepositoryInterface>()
    private val knownEntitiesService = mockk<KnownEntitiesService>()
    private val service = BookingService(repository, knownEntitiesService)

    private val booking = Booking(id = "b1", userId = "u1", carId = "c1")
    private val request = BookingRequest(userId = "u1", carId = "c1")

    // --- create() ---

    @Test
    fun `create - success`() = runTest {
        coEvery { knownEntitiesService.userExists("u1") } returns true
        coEvery { knownEntitiesService.carExists("c1") } returns true
        coEvery { repository.findByCarId("c1") } returns null
        coEvery { repository.insert("u1", "c1") } returns booking

        val result = service.create(request)

        assertEquals(booking, result)
        coVerify(exactly = 1) { repository.insert("u1", "c1") }
    }

    @Test
    fun `create - throws UserNotFoundException when user unknown`() = runTest {
        coEvery { knownEntitiesService.userExists("u1") } returns false

        assertFailsWith<UserNotFoundException> {
            service.create(request)
        }
        coVerify(exactly = 0) { repository.insert(any(), any()) }
    }

    @Test
    fun `create - throws CarNotFoundException when car unknown`() = runTest {
        coEvery { knownEntitiesService.userExists("u1") } returns true
        coEvery { knownEntitiesService.carExists("c1") } returns false

        assertFailsWith<CarNotFoundException> {
            service.create(request)
        }
        coVerify(exactly = 0) { repository.insert(any(), any()) }
    }

    @Test
    fun `create - throws CarAlreadyBookedException when car already booked`() = runTest {
        coEvery { knownEntitiesService.userExists("u1") } returns true
        coEvery { knownEntitiesService.carExists("c1") } returns true
        coEvery { repository.findByCarId("c1") } returns booking

        assertFailsWith<CarAlreadyBookedException> {
            service.create(request)
        }
        coVerify(exactly = 0) { repository.insert(any(), any()) }
    }

    // --- findById() ---

    @Test
    fun `findById - returns booking when found`() = runTest {
        coEvery { repository.findById("b1") } returns booking

        val result = service.findById("b1")

        assertEquals(booking, result)
    }

    @Test
    fun `findById - returns null when not found`() = runTest {
        coEvery { repository.findById("unknown") } returns null

        val result = service.findById("unknown")

        assertNull(result)
    }

    // --- cancel() ---

    @Test
    fun `cancel - success`() = runTest {
        coEvery { repository.deleteById("b1") } returns booking

        val result = service.cancel("b1")

        assertEquals(booking, result)
        coVerify(exactly = 1) { repository.deleteById("b1") }
    }

    @Test
    fun `cancel - throws CarNotBookedException when booking not found`() = runTest {
        coEvery { repository.deleteById("unknown") } returns null

        assertFailsWith<CarNotBookedException> {
            service.cancel("unknown")
        }
    }

    // --- findByUser() ---

    @Test
    fun `findByUser - returns all bookings for user`() = runTest {
        val bookings = listOf(
            Booking(id = "b1", userId = "u1", carId = "c1"),
            Booking(id = "b2", userId = "u1", carId = "c2")
        )
        coEvery { repository.findByUserId("u1") } returns bookings

        val result = service.findByUser("u1")

        assertEquals(2, result.size)
        assertEquals(bookings, result)
    }

    @Test
    fun `findByUser - returns empty list when user has no bookings`() = runTest {
        coEvery { repository.findByUserId("u1") } returns emptyList()

        val result = service.findByUser("u1")

        assertEquals(emptyList(), result)
    }
}
