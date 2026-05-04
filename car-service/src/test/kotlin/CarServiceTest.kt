package at.ac.hcw

import at.ac.hcw.domain.Car
import at.ac.hcw.domain.FuelType
import at.ac.hcw.domain.Transmission
import at.ac.hcw.dto.CarPatchRequest
import at.ac.hcw.dto.Location
import at.ac.hcw.repository.CarRepository
import at.ac.hcw.service.CarService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CarServiceTest {

    private val repository = mockk<CarRepository>()
    private val service = CarService(repository)

    private fun car(id: String = "c1", available: Boolean = true) = Car(
        id = id,
        manufacturer = "Toyota",
        model = "Corolla",
        year = 2022,
        pricePerDay = 50.0,
        description = "A reliable car",
        imageName = "cars/c1.jpg",
        transmission = Transmission.AUTOMATIC,
        power = 132,
        fuelType = FuelType.GASOLINE,
        available = available,
        location = Location(48.2, 16.4)
    )

    // --- getAllCars() ---

    @Test
    fun `getAllCars - returns all cars from repository`() = runTest {
        val cars = listOf(car("c1"), car("c2"))
        coEvery { repository.findAll() } returns cars

        val result = service.getAllCars()

        assertEquals(cars, result)
    }

    @Test
    fun `getAllCars - returns empty list when no cars exist`() = runTest {
        coEvery { repository.findAll() } returns emptyList()

        val result = service.getAllCars()

        assertEquals(emptyList(), result)
    }

    // --- getAllAvailableCars() ---

    @Test
    fun `getAllAvailableCars - returns only available cars`() = runTest {
        val cars = listOf(car("c1", available = true))
        coEvery { repository.findAllAvailable() } returns cars

        val result = service.getAllAvailableCars()

        assertEquals(cars, result)
        coVerify(exactly = 1) { repository.findAllAvailable() }
    }

    // --- getCar() ---

    @Test
    fun `getCar - returns car when found`() = runTest {
        val expected = car("c1")
        coEvery { repository.findById("c1") } returns expected

        val result = service.getCar("c1")

        assertEquals(expected, result)
    }

    @Test
    fun `getCar - returns null when not found`() = runTest {
        coEvery { repository.findById("unknown") } returns null

        val result = service.getCar("unknown")

        assertNull(result)
    }

    // --- createCar() ---

    @Test
    fun `createCar - returns created id from repository`() = runTest {
        val newCar = car(id = "")
        coEvery { repository.create(newCar) } returns "new-id"

        val result = service.createCar(newCar)

        assertEquals("new-id", result)
        coVerify(exactly = 1) { repository.create(newCar) }
    }

    // --- patchCar() ---

    @Test
    fun `patchCar - returns updated car when found`() = runTest {
        val patch = CarPatchRequest(model = "Camry")
        val updated = car().also { it.model = "Camry" }
        coEvery { repository.patch("c1", patch) } returns updated

        val result = service.patchCar("c1", patch)

        assertEquals(updated, result)
    }

    @Test
    fun `patchCar - returns null when car not found`() = runTest {
        val patch = CarPatchRequest(model = "Camry")
        coEvery { repository.patch("unknown", patch) } returns null

        val result = service.patchCar("unknown", patch)

        assertNull(result)
    }

    // --- deleteCar() ---

    @Test
    fun `deleteCar - returns true when deletion succeeds`() = runTest {
        coEvery { repository.delete("c1") } returns true

        val result = service.deleteCar("c1")

        assertTrue(result)
    }

    @Test
    fun `deleteCar - returns false when car does not exist`() = runTest {
        coEvery { repository.delete("unknown") } returns false

        val result = service.deleteCar("unknown")

        assertFalse(result)
    }

    // --- markCarAsBooked() ---

    @Test
    fun `markCarAsBooked - sets availability to false`() = runTest {
        coEvery { repository.setAvailability("c1", false) } returns true

        val result = service.markCarAsBooked("c1")

        assertTrue(result)
        coVerify(exactly = 1) { repository.setAvailability("c1", false) }
    }

    @Test
    fun `markCarAsBooked - returns false when car not found`() = runTest {
        coEvery { repository.setAvailability("unknown", false) } returns false

        val result = service.markCarAsBooked("unknown")

        assertFalse(result)
    }

    // --- markCarAsAvailable() ---

    @Test
    fun `markCarAsAvailable - sets availability to true`() = runTest {
        coEvery { repository.setAvailability("c1", true) } returns true

        val result = service.markCarAsAvailable("c1")

        assertTrue(result)
        coVerify(exactly = 1) { repository.setAvailability("c1", true) }
    }

    @Test
    fun `markCarAsAvailable - returns false when car not found`() = runTest {
        coEvery { repository.setAvailability("unknown", true) } returns false

        val result = service.markCarAsAvailable("unknown")

        assertFalse(result)
    }
}
