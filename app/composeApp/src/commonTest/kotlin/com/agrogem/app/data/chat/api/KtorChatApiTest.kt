package com.agrogem.app.data.chat.api

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

class KtorChatApiTest {

    @Test
    fun `sendMessage posts to chat messages with session id and message payload`() = runTest {
        val mockEngine = MockEngine { request ->
            assertTrue(request.url.encodedPath.endsWith("/chat/messages"))
            respond(
                content = """
                    {
                        "id": "+50255550000",
                        "messages": [
                            {
                                "role": "user",
                                "content": "Hola, necesito ayuda con mi cultivo de maíz",
                                "created_at": "2026-04-24T19:35:00Z"
                            }
                        ],
                        "created_at": "2026-04-24T19:30:00Z",
                        "updated_at": "2026-04-24T19:35:00Z"
                    }
                """.trimIndent(),
                status = HttpStatusCode.Created,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val api: ChatApi = KtorChatApi(HttpClientFactory.create(engine = mockEngine))

        val result = api.sendMessage(
            sessionId = "c3d4e5f6-a7b8-9012-cdef-123456789012",
            content = "Hola, necesito ayuda con mi cultivo de maíz"
        )

        assertEquals("+50255550000", result.id)
        assertEquals(1, result.messages.size)
        assertEquals("user", result.messages[0].role)
        assertEquals("Hola, necesito ayuda con mi cultivo de maíz", result.messages[0].content)
        assertEquals("2026-04-24T19:35:00Z", result.messages[0].createdAt)
        assertEquals("2026-04-24T19:30:00Z", result.createdAt)
        assertEquals("2026-04-24T19:35:00Z", result.updatedAt)
    }

    @Test
    fun `sendMessage returns conversation with assistant reply`() = runTest {
        val mockEngine = MockEngine { request ->
            assertTrue(request.url.encodedPath.endsWith("/chat/messages"))
            respond(
                content = """
                    {
                        "id": "+50255550000",
                        "messages": [
                            {
                                "role": "user",
                                "content": "¿Cómo trato la roya del café?",
                                "created_at": "2026-04-24T19:35:00Z"
                            },
                            {
                                "role": "assistant",
                                "content": "Te recomiendo empezar por verificar el nivel de infección.",
                                "created_at": "2026-04-24T19:35:01Z"
                            }
                        ],
                        "created_at": "2026-04-24T19:30:00Z",
                        "updated_at": "2026-04-24T19:35:01Z"
                    }
                """.trimIndent(),
                status = HttpStatusCode.Created,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val api: ChatApi = KtorChatApi(HttpClientFactory.create(engine = mockEngine))

        val result = api.sendMessage(
            sessionId = "c3d4e5f6-a7b8-9012-cdef-123456789012",
            content = "¿Cómo trato la roya del café?"
        )

        assertEquals(2, result.messages.size)
        assertEquals("assistant", result.messages[1].role)
        assertEquals("Te recomiendo empezar por verificar el nivel de infección.", result.messages[1].content)
    }

    @Test
    fun `sendMessage returns NotFound on 404`() = runTest {
        val mockEngine = MockEngine {
            respondError(status = HttpStatusCode.NotFound)
        }

        val api: ChatApi = KtorChatApi(HttpClientFactory.create(engine = mockEngine))

        val error = runCatching {
            api.sendMessage(sessionId = "expired-session", content = "test")
        }.exceptionOrNull()

        assertIs<ApiError.NotFound>(error)
    }

    @Test
    fun `sendMessage returns Validation on 422`() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = "Invalid message payload",
                status = HttpStatusCode.UnprocessableEntity,
            )
        }

        val api: ChatApi = KtorChatApi(HttpClientFactory.create(engine = mockEngine))

        val error = runCatching {
            api.sendMessage(sessionId = "valid-session", content = "")
        }.exceptionOrNull()

        assertIs<ApiError.Validation>(error)
        assertEquals("Invalid message payload", error.message)
    }

    @Test
    fun `sendMessage returns ServerError on 500`() = runTest {
        val mockEngine = MockEngine {
            respondError(status = HttpStatusCode.InternalServerError)
        }

        val api: ChatApi = KtorChatApi(HttpClientFactory.create(engine = mockEngine))

        val error = runCatching {
            api.sendMessage(sessionId = "valid-session", content = "test")
        }.exceptionOrNull()

        assertIs<ApiError.ServerError>(error)
    }

    @Test
    fun `sendMessage returns NetworkError on engine failure`() = runTest {
        val mockEngine = MockEngine {
            throw RuntimeException("No connection")
        }

        val api: ChatApi = KtorChatApi(HttpClientFactory.create(engine = mockEngine))

        val error = runCatching {
            api.sendMessage(sessionId = "valid-session", content = "test")
        }.exceptionOrNull()

        assertIs<ApiError.NetworkError>(error)
    }
}
