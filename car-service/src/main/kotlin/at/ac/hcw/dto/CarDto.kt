package at.ac.hcw.dto

import at.ac.hcw.domain.Car
import kotlinx.serialization.Serializable

@Serializable
data class CarEvent(val carId: String)

@Serializable
data class CarCreateRequest(
    val manufacturer: String,
    val model: String,
    val year: Int,
    val pricePerDay: Double,
    val description: String,
    val imageName: String,
    val transmission: String,
    val power: Int,
    val fuelType: String
)

@Serializable
data class CarPatchRequest(
    val manufacturer: String? = null,
    val model: String? = null,
    val year: Int? = null,
    val pricePerDay: Double? = null,
    val description: String? = null,
    val imageName: String? = null,
    val transmission: String? = null,
    val power: Int? = null,
    val fuelType: String? = null
) {
}

@Serializable
data class CarResponse(
    val id: String,
    val manufacturer: String,
    val model: String,
    val year: Int,
    val pricePerDay: Double,
    val description: String,
    val imageName: String,
    val transmission: String,
    val power: Int,
    val fuelType: String,
    val available: Boolean
)

fun CarCreateRequest.toDomain(): Car =
    Car(
        manufacturer = manufacturer,
        model = model,
        year = year,
        pricePerDay = pricePerDay,
        description = description,
        imageName = imageName,
        transmission = transmission,
        power = power,
        fuelType = fuelType,
        available = true
    )

fun Car.toResponse(): CarResponse =
    CarResponse(
        id = id ?: "",
        manufacturer = manufacturer,
        model = model,
        year = year,
        pricePerDay = pricePerDay,
        description = description,
        imageName = imageName,
        transmission = transmission,
        power = power,
        fuelType = fuelType,
        available = available
    )