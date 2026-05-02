package at.ac.hcw.domain

import kotlinx.serialization.Serializable

@Serializable
enum class FuelType {
    GASOLINE,
    DIESEL,
    ELECTRIC,
    HYBRID
}
