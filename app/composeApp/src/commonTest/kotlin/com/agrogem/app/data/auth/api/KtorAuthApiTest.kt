package com.agrogem.app.data.auth.api

import com.agrogem.app.data.network.ApiError
import com.agrogem.app.data.network.HttpClientFactory
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class KtorAuthApiTest {

    @Test
    fun `register posts to users register with phone and password`() = runTest {
        val mockEngine = MockEngine { request ->
            assertTrue(request.url.encodedPath.endsWith("/users/register"))
            respond(
                content = """{"session_id":"sess-reg-001"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val api: AuthApi = KtorAuthApi(HttpClientFactory.create(engine = mockEngine))

        val result = api.register(phone = "+50255550000", password = "secret123")

        assertEquals("sess-reg-001", result.sessionId)
    }

    @Test
    fun `login posts to users login with phone and password`() = runTest {
        val mockEngine = MockEngine { request ->
            assertTrue(request.url.encodedPath.endsWith("/users/login"))
            respond(
                content = """{"session_id":"sess-login-002"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val api: AuthApi = KtorAuthApi(HttpClientFactory.create(engine = mockEngine))

        val result = api.login(phone = "+50255550000", password = "secret123")

        assertEquals("sess-login-002", result.sessionId)
    }

    @Test
    fun `register returns DuplicateRegistration on 409`() = runTest {
        val mockEngine = MockEngine {
            respondError(status = HttpStatusCode.Conflict)
        }

        val api: AuthApi = KtorAuthApi(HttpClientFactory.create(engine = mockEngine))

        val error = runCatching { api.register(phone = "+50255550000", password = "secret123") }.exceptionOrNull()

        assertIs<ApiError.DuplicateRegistration>(error)
    }

    @Test
    fun `login returns NotFound on 404`() = runTest {
        val mockEngine = MockEngine {
            respondError(status = HttpStatusCode.NotFound)
        }

        val api: AuthApi = KtorAuthApi(HttpClientFactory.create(engine = mockEngine))

        val error = runCatching { api.login(phone = "+50255550000", password = "secret123") }.exceptionOrNull()

        assertIs<ApiError.NotFound>(error)
    }

    @Test
    fun `login returns Unauthorized on 401`() = runTest {
        val mockEngine = MockEngine {
            respondError(status = HttpStatusCode.Unauthorized)
        }

        val api: AuthApi = KtorAuthApi(HttpClientFactory.create(engine = mockEngine))

        val error = runCatching { api.login(phone = "+50255550000", password = "secret123") }.exceptionOrNull()

        assertIs<ApiError.Unauthorized>(error)
    }

    @Test
    fun `register returns Validation on 422`() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = "Phone invalid",
                status = HttpStatusCode.UnprocessableEntity,
            )
        }

        val api: AuthApi = KtorAuthApi(HttpClientFactory.create(engine = mockEngine))

        val error = runCatching { api.register(phone = "bad", password = "") }.exceptionOrNull()

        assertIs<ApiError.Validation>(error)
        assertEquals("Phone invalid", error.message)
    }

    @Test
    fun `login returns ServerError on 500`() = runTest {
        val mockEngine = MockEngine {
            respondError(status = HttpStatusCode.InternalServerError)
        }

        val api: AuthApi = KtorAuthApi(HttpClientFactory.create(engine = mockEngine))

        val error = runCatching { api.login(phone = "+50255550000", password = "secret123") }.exceptionOrNull()

        assertIs<ApiError.ServerError>(error)
    }

    @Test
    fun `login returns DuplicateRegistration on 409`() = runTest {
        val mockEngine = MockEngine {
            respondError(status = HttpStatusCode.Conflict)
        }

        val api: AuthApi = KtorAuthApi(HttpClientFactory.create(engine = mockEngine))

        val error = runCatching { api.login(phone = "+50255550000", password = "secret123") }.exceptionOrNull()

        assertIs<ApiError.DuplicateRegistration>(error)
    }

    @Test
    fun `login returns Validation on 422`() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = "Phone required",
                status = HttpStatusCode.UnprocessableEntity,
            )
        }

        val api: AuthApi = KtorAuthApi(HttpClientFactory.create(engine = mockEngine))

        val error = runCatching { api.login(phone = "", password = "") }.exceptionOrNull()

        assertIs<ApiError.Validation>(error)
        assertEquals("Phone required", error.message)
    }

    @Test
    fun `login returns NetworkError on engine failure`() = runTest {
        val mockEngine = MockEngine {
            throw RuntimeException("No connection")
        }

        val api: AuthApi = KtorAuthApi(HttpClientFactory.create(engine = mockEngine))

        val error = runCatching { api.login(phone = "+50255550000", password = "secret123") }.exceptionOrNull()

        assertIs<ApiError.NetworkError>(error)
    }
}
