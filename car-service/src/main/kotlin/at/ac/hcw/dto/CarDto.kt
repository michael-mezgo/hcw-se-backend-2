package at.ac.hcw.dto

import at.ac.hcw.domain.Car
import com.google.protobuf.Empty
import currency.Currency
import currency.CurrencyServiceGrpcKt
import io.ktor.server.plugins.BadRequestException
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
    val pricePerDay: CurrencyDto,
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

suspend fun CurrencyServiceGrpcKt.CurrencyServiceCoroutineStub.getSupportedCurrenciesList(): List<String> =
    getSupportedCurrencies(Empty.getDefaultInstance()).currenciesList

suspend fun Car.toResponse(
    currencyService: CurrencyServiceGrpcKt.CurrencyServiceCoroutineStub? = null,
    toCurrency: String = "USD"
): CarResponse {
    val currencies = currencyService?.getSupportedCurrenciesList()
    if (currencies?.contains(toCurrency) ?: false) {
        println("Currency $toCurrency is supported, converting price.")
    } else {
        println("Currency $toCurrency is not supported")
        throw BadRequestException("Currency $toCurrency is not supported.")
    }
    val price = if (toCurrency != "USD") {
        CurrencyDto(
            amount = currencyService.convert(
                Currency.ConvertRequest.newBuilder()
                    .setFromCurrency("USD")
                    .setToCurrency(toCurrency)
                    .setAmount(pricePerDay)
                    .build()
            ).result,
            currencyCode = toCurrency
        )
    } else {
        CurrencyDto(amount = pricePerDay, currencyCode = "USD")
    }
    return CarResponse(
        id = id ?: "",
        manufacturer = manufacturer,
        model = model,
        year = year,
        pricePerDay = price,
        description = description,
        imageName = imageName,
        transmission = transmission,
        power = power,
        fuelType = fuelType,
        available = available
    )
}