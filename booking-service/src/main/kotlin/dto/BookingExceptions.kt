package at.ac.hcw.dto

class UserNotFoundException(userId: String) : Exception("User not found: $userId")
class CarNotFoundException(carId: String) : Exception("Car not found: $carId")
class CarAlreadyBookedException(carId: String) : Exception("Car is already booked: $carId")
