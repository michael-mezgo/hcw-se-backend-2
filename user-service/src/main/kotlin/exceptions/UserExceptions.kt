package at.ac.hcw.exceptions

class UserNotFoundException(userId: String) : Exception("User not found: $userId")
class UserExistsException(userId: String) : Exception("User already exists: $userId")
class UnauthorizedException(userId: String) : Exception("Invalid credentials: $userId")
class ForbiddenException(userId: String) : Exception("Access denied: $userId")