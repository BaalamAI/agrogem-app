package com.agrogem.app.data.network

import io.ktor.http.HttpStatusCode

sealed class ApiError : Exception() {
    data object DuplicateRegistration : ApiError()
    data object NotFound : ApiError()
    data class Validation(override val message: String) : ApiError()
    data object Unauthorized : ApiError()
    data object ServerError : ApiError()
    data class NetworkError(override val cause: Throwable) : ApiError()

    companion object {
        fun from(statusCode: HttpStatusCode, body: String? = null): ApiError = when (statusCode.value) {
            409 -> DuplicateRegistration
            404 -> NotFound
            422 -> Validation(body ?: "Validation failed")
            401 -> Unauthorized
            in 500..599 -> ServerError
            else -> ServerError
        }

        fun from(cause: Throwable): ApiError = NetworkError(cause)
    }
}
