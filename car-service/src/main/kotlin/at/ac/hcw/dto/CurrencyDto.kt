package at.ac.hcw.dto

import kotlinx.serialization.Serializable

@Serializable
data class CurrencyDto(
    val amount: Double,
    val currencyCode: String,
)