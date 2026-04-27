package com.agrogem.app.data.network

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.HttpStatusCode
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HttpClientFactoryTest {

    @Serializable
    data class TestRequest(val name: String)

    @Serializable
    data class TestResponse(val id: String)

    @Test
    fun `client serializes request body as JSON`() = runTest {
        val mockEngine = MockEngine { request ->
            assertEquals(ContentType.Application.Json, request.body.contentType)
            respond(
                content = """{"id":"abc123"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val client = HttpClientFactory.create(engine = mockEngine)

        client.post("/test") {
            setBody(TestRequest(name = "hello"))
        }.body<TestResponse>()
    }

    @Test
    fun `client deserializes JSON response body`() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = """{"id":"xyz789"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val client = HttpClientFactory.create(engine = mockEngine)

        val response = client.post("/test") {
            setBody(TestRequest(name = "hello"))
        }.body<TestResponse>()

        assertEquals("xyz789", response.id)
    }

    @Test
    fun `client applies base URL to requests`() = runTest {
        val mockEngine = MockEngine { request ->
            assertTrue(request.url.toString().startsWith(BackendConfig.BASE_URL))
            respond("", status = HttpStatusCode.OK)
        }

        val client = HttpClientFactory.create(engine = mockEngine)

        client.post("/register")
    }

    @Test
    fun `client does not throw on non-2xx responses`() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = """{"error":"not found"}""",
                status = HttpStatusCode.NotFound,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val client = HttpClientFactory.create(engine = mockEngine)

        val response = client.post("/test")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `client with logging enabled handles requests normally`() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = """{"id":"logged"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val client = HttpClientFactory.create(engine = mockEngine, loggingEnabled = true)

        val response = client.post("/test") {
            setBody(TestRequest(name = "hello"))
        }.body<TestResponse>()

        assertEquals("logged", response.id)
    }
}
