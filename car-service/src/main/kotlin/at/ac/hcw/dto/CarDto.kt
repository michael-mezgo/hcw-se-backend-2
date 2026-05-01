package at.ac.hcw.dto

import at.ac.hcw.CurrencyClient
import at.ac.hcw.service.BlobStorageService
import at.ac.hcw.domain.Car
import at.ac.hcw.domain.FuelType
import com.google.protobuf.Empty
import currency.Currency
import currency.CurrencyServiceGrpcKt
import io.grpc.Status
import io.grpc.StatusException
import io.ktor.server.plugins.BadRequestException
import kotlinx.serialization.Serializable
import javax.naming.ServiceUnavailableException

@Serializable
data class CarCreateRequest(
    val manufacturer: String,
    val model: String,
    val year: Int,
    val pricePerDay: Double,
    val description: String,
    val transmission: String,
    val power: Int,
    val fuelType: FuelType
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
    val fuelType: FuelType? = null
) {
}

@Serializable
data class CarResponse(
    val id: String,
    val manufacturer: String,
    val model: String,
    val year: Int,
    val pricePerDay: CurrencyDto,
    val description: String,
    val imageName: String,
    val transmission: String,
    val power: Int,
    val fuelType: FuelType,
    val isAvailable: Boolean
)

fun CarCreateRequest.toDomain(imageName: String): Car =
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

private suspend fun CurrencyServiceGrpcKt.CurrencyServiceCoroutineStub.getSupportedCurrenciesList(): List<String> =
    getSupportedCurrencies(Empty.getDefaultInstance()).currenciesList

suspend fun Car.toResponse(
    currencyService: CurrencyClient? = null,
    toCurrency: String = "USD",
    blobStorageService: BlobStorageService? = null
): CarResponse {
    val price: CurrencyDto
    if (toCurrency.uppercase() == "USD")
        price = CurrencyDto(amount = pricePerDay, currencyCode = "USD")
    else {
        if (currencyService == null)
            throw ServiceUnavailableException("Currency conversion not available.")

        val currencyName = toCurrency.uppercase().trim()
        if (!currencyService.stub.getSupportedCurrenciesList().contains(currencyName))
            throw BadRequestException("Currency $currencyName is not supported.")

        val request: Currency.ConvertRequest = Currency.ConvertRequest.newBuilder()
            .setAmount(pricePerDay)
            .setFromCurrency("USD")
            .setToCurrency(currencyName)
            .setApiKey(currencyService.apiKey)
            .build()

        try {
            val response = currencyService.stub.convert(request)
            price = CurrencyDto(amount = response.result, currencyCode = currencyName)
        } catch (e: StatusException) {
            if (e.status.code == Status.Code.UNAUTHENTICATED)
                throw ServiceUnavailableException("Currency service authentication failed: invalid API key.")
            throw e
        }
    }

    var imageUrl: String? = null
    if (blobStorageService != null) {
        imageUrl = blobStorageService.getUrl(imageName)
    }


    return CarResponse(
        id = id ?: "",
        manufacturer = manufacturer,
        model = model,
        year = year,
        pricePerDay = price,
        description = description,
        imageName = imageUrl?: "",
        transmission = transmission,
        power = power,
        fuelType = fuelType,
        isAvailable = available
    )
}