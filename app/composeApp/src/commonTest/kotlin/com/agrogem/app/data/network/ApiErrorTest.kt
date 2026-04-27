package com.agrogem.app.data.network

import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ApiErrorTest {

    @Test
    fun `status 409 maps to DuplicateRegistration`() {
        val error = ApiError.from(HttpStatusCode.Conflict)

        assertIs<ApiError.DuplicateRegistration>(error)
    }

    @Test
    fun `status 404 maps to NotFound`() {
        val error = ApiError.from(HttpStatusCode.NotFound)

        assertIs<ApiError.NotFound>(error)
    }

    @Test
    fun `status 422 maps to Validation with message`() {
        val error = ApiError.from(HttpStatusCode.UnprocessableEntity, body = "Phone invalid")

        assertIs<ApiError.Validation>(error)
        assertEquals("Phone invalid", error.message)
    }

    @Test
    fun `status 422 without body uses default message`() {
        val error = ApiError.from(HttpStatusCode.UnprocessableEntity)

        assertIs<ApiError.Validation>(error)
        assertTrue(error.message.isNotBlank())
    }

    @Test
    fun `status 500 maps to ServerError`() {
        val error = ApiError.from(HttpStatusCode.InternalServerError)

        assertIs<ApiError.ServerError>(error)
    }

    @Test
    fun `status 502 maps to ServerError`() {
        val error = ApiError.from(HttpStatusCode.BadGateway)

        assertIs<ApiError.ServerError>(error)
    }

    @Test
    fun `status 599 maps to ServerError`() {
        val error = ApiError.from(HttpStatusCode(599, "Server Error"))

        assertIs<ApiError.ServerError>(error)
    }

    @Test
    fun `status 400 maps to ServerError`() {
        val error = ApiError.from(HttpStatusCode.BadRequest)

        assertIs<ApiError.ServerError>(error)
    }

    @Test
    fun `status 401 maps to Unauthorized`() {
        val error = ApiError.from(HttpStatusCode.Unauthorized)

        assertIs<ApiError.Unauthorized>(error)
    }

    @Test
    fun `status 401 with body still maps to Unauthorized`() {
        val error = ApiError.from(HttpStatusCode.Unauthorized, body = "Token expired")

        assertIs<ApiError.Unauthorized>(error)
    }

    @Test
    fun `IOException maps to NetworkError`() {
        class TestIOException(message: String) : Exception(message)

        val cause = TestIOException("No connection")

        val error = ApiError.from(cause)

        assertIs<ApiError.NetworkError>(error)
        assertEquals("No connection", error.cause.message)
    }

    @Test
    fun `RuntimeException maps to NetworkError`() {
        val cause = RuntimeException("Unexpected")

        val error = ApiError.from(cause)

        assertIs<ApiError.NetworkError>(error)
        assertEquals("Unexpected", error.cause.message)
    }
}
