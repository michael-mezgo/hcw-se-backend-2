package at.ac.hcw.domain

class Car (
    val id: String? = null,
    val manufacturer: String,
    val model: String,
    val year: Int,
    val pricePerDay: Double,
    val description: String,
    val imageName: String,
    val transmission: String,
    val power: Int,
    val fuelType: String,
    val available: Boolean = false
)