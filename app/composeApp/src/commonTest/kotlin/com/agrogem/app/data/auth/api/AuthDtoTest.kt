package com.agrogem.app.data.auth.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `RegisterRequest serializes to JSON with phone and password`() {
        val request = RegisterRequest(phone = "+50255550000", password = "secret123")

        val encoded = json.encodeToString(request)

        assertTrue(encoded.contains("\"phone\":\"+50255550000\""))
        assertTrue(encoded.contains("\"password\":\"secret123\""))
    }

    @Test
    fun `RegisterRequest deserializes from JSON`() {
        val body = """{"phone":"+50255550000","password":"secret123"}"""

        val decoded = json.decodeFromString<RegisterRequest>(body)

        assertEquals("+50255550000", decoded.phone)
        assertEquals("secret123", decoded.password)
    }

    @Test
    fun `LoginRequest serializes to JSON with phone and password`() {
        val request = LoginRequest(phone = "+50255550000", password = "secret123")

        val encoded = json.encodeToString(request)

        assertTrue(encoded.contains("\"phone\":\"+50255550000\""))
        assertTrue(encoded.contains("\"password\":\"secret123\""))
    }

    @Test
    fun `LoginRequest deserializes from JSON`() {
        val body = """{"phone":"+50255550000","password":"secret123"}"""

        val decoded = json.decodeFromString<LoginRequest>(body)

        assertEquals("+50255550000", decoded.phone)
        assertEquals("secret123", decoded.password)
    }

    @Test
    fun `SessionResponse deserializes session_id`() {
        val body = """{"session_id":"sess-abc-123"}"""

        val decoded = json.decodeFromString<SessionResponse>(body)

        assertEquals("sess-abc-123", decoded.sessionId)
    }

    @Test
    fun `SessionResponse serializes session_id`() {
        val response = SessionResponse(sessionId = "sess-abc-123")

        val encoded = json.encodeToString(response)

        assertTrue(encoded.contains("\"session_id\":\"sess-abc-123\""))
    }

    @Test
    fun `SessionResponse ignores unknown keys during deserialization`() {
        val body = """{"session_id":"sess-xyz-789","extra_field":"ignored"}"""

        val decoded = json.decodeFromString<SessionResponse>(body)

        assertEquals("sess-xyz-789", decoded.sessionId)
    }

    @Test
    fun `RegisterRequest handles empty strings`() {
        val request = RegisterRequest(phone = "", password = "")

        val encoded = json.encodeToString(request)
        val decoded = json.decodeFromString<RegisterRequest>(encoded)

        assertEquals("", decoded.phone)
        assertEquals("", decoded.password)
    }
}
