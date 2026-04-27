package com.agrogem.app.data.pest.api

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

class KtorPestApiTest {

    @Test
    fun `getUploadUrl posts to pest upload-url and returns response fields`() = runTest {
        val mockEngine = MockEngine { request ->
            assertTrue(request.url.encodedPath.endsWith("/pest/upload-url"))
            respond(
                content = """{"object_path":"pests/user-123/image.jpg","signed_url":"https://storage.example.com/upload?token=abc","content_type":"image/jpeg","expires_in_seconds":300}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val api: PestApi = KtorPestApi(HttpClientFactory.create(engine = mockEngine))

        val result = api.getUploadUrl()

        assertEquals("pests/user-123/image.jpg", result.objectPath)
        assertEquals("https://storage.example.com/upload?token=abc", result.signedUrl)
        assertEquals("image/jpeg", result.contentType)
        assertEquals(300, result.expiresInSeconds)
    }

    @Test
    fun `uploadImage does PUT with binary body and jpeg content type`() = runTest {
        val mockEngine = MockEngine { request ->
            assertEquals("PUT", request.method.value)
            assertTrue(request.url.toString().contains("storage.example.com/upload"))
            assertTrue(request.url.toString().contains("token=abc"))
            assertEquals(ContentType.Image.JPEG, request.body.contentType)
            respond(
                content = "",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val api: PestApi = KtorPestApi(HttpClientFactory.create(engine = mockEngine))

        api.uploadImage(signedUrl = "https://storage.example.com/upload?token=abc", imageBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()))
    }

    @Test
    fun `identify posts object path and returns top match`() = runTest {
        val mockEngine = MockEngine { request ->
            assertTrue(request.url.encodedPath.endsWith("/pest/identify"))
            respond(
                content = """
                    {
                        "top_match": {
                            "pest_name": "Spodoptera_litura",
                            "similarity": 0.87,
                            "confidence": "high"
                        },
                        "votes": {"Spodoptera_litura": 3}
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val api: PestApi = KtorPestApi(HttpClientFactory.create(engine = mockEngine))

        val result = api.identify(objectPath = "pests/user-123/image.jpg")

        assertEquals("Spodoptera_litura", result.topMatch?.pestName)
        assertEquals(0.87, result.topMatch?.similarity)
        assertEquals("high", result.topMatch?.confidence)
        assertEquals(mapOf("Spodoptera_litura" to 3), result.votes)
    }

    @Test
    fun `identify returns null top match when absent`() = runTest {
        val mockEngine = MockEngine { request ->
            assertTrue(request.url.encodedPath.endsWith("/pest/identify"))
            respond(
                content = "{}",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val api: PestApi = KtorPestApi(HttpClientFactory.create(engine = mockEngine))

        val result = api.identify(objectPath = "pests/user-123/image.jpg")

        assertEquals(null, result.topMatch)
        assertEquals(emptyMap(), result.votes)
    }

    @Test
    fun `getUploadUrl returns ServerError on 500`() = runTest {
        val mockEngine = MockEngine {
            respondError(status = HttpStatusCode.InternalServerError)
        }

        val api: PestApi = KtorPestApi(HttpClientFactory.create(engine = mockEngine))

        val error = runCatching { api.getUploadUrl() }.exceptionOrNull()

        assertIs<ApiError.ServerError>(error)
    }

    @Test
    fun `uploadImage returns NetworkError on engine failure`() = runTest {
        val mockEngine = MockEngine {
            throw RuntimeException("No connection")
        }

        val api: PestApi = KtorPestApi(HttpClientFactory.create(engine = mockEngine))

        val error = runCatching {
            api.uploadImage(signedUrl = "https://storage.example.com/upload", imageBytes = byteArrayOf())
        }.exceptionOrNull()

        assertIs<ApiError.NetworkError>(error)
    }

    @Test
    fun `identify returns ServerError on 500`() = runTest {
        val mockEngine = MockEngine {
            respondError(status = HttpStatusCode.InternalServerError)
        }

        val api: PestApi = KtorPestApi(HttpClientFactory.create(engine = mockEngine))

        val error = runCatching { api.identify(objectPath = "path") }.exceptionOrNull()

        assertIs<ApiError.ServerError>(error)
    }
}
