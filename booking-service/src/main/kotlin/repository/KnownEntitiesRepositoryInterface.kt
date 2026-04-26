package at.ac.hcw.repository

interface KnownEntitiesRepositoryInterface {
    suspend fun insertUser(userId: String)
    suspend fun deleteUser(userId: String)
    suspend fun findUser(userId: String): Boolean
    suspend fun insertCar(carId: String)
    suspend fun deleteCar(carId: String)
    suspend fun findCar(carId: String): Boolean
}
