package com.agrogem.app.data.auth.domain

/**
 * Typed result wrapper for auth operations.
 */
sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Duplicate(val message: String = "Phone already registered") : AuthResult<Nothing>()
    data class NotFound(val message: String = "Session not found") : AuthResult<Nothing>()
    data class Validation(val message: String) : AuthResult<Nothing>()
    data class Unauthorized(val message: String = "Unauthorized") : AuthResult<Nothing>()
    data class Server(val message: String = "Server error") : AuthResult<Nothing>()
    data class Network(val cause: Throwable) : AuthResult<Nothing>()
}
