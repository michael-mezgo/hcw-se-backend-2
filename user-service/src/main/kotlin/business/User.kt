package at.ac.hcw.business

data class User(
    val id: String,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val licenseNumber: String,
    val licenseValidUntil: String,
    val isAdmin: Boolean,
    val isLocked: Boolean,
)