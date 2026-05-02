package at.ac.hcw.domain

import at.ac.hcw.dto.Location

class Car (
    var id: String? = null,
    var manufacturer: String = "",
    var model: String = "",
    var year: Int = 0,
    var pricePerDay: Double = 0.0,
    var description: String = "",
    var imageName: String = "",
    var transmission: Transmission = Transmission.AUTOMATIC,
    var power: Int = 0,
    var fuelType: FuelType = FuelType.GASOLINE,
    var available: Boolean = false,
    var location: Location = Location(0.0, 0.0)
)
