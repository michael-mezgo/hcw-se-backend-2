package at.ac.hcw.domain

class Car (
    var id: String? = null,
    var manufacturer: String = "",
    var model: String = "",
    var year: Int = 0,
    var pricePerDay: Double = 0.0,
    var description: String = "",
    var imageName: String = "",
    var transmission: String = "",
    var power: Int = 0,
    var fuelType: FuelType = FuelType.GASOLINE,
    var available: Boolean = false
)
