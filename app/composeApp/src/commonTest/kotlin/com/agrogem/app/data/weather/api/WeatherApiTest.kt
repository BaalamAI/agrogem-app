package com.agrogem.app.data.weather.api

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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WeatherApiTest {

    @Test
    fun `getCurrentWeather returns full response with lat lon params`() = runTest {
        val mockEngine = MockEngine { request ->
            assertTrue(request.url.encodedPath.endsWith("/weather"))
            assertEquals("14.9726", request.url.parameters["lat"])
            assertEquals("-89.5301", request.url.parameters["lon"])
            respond(
                content = """{
                    "latitude": 14.9726,
                    "longitude": -89.5301,
                    "timezone": "America/Guatemala",
                    "current": {
                        "time": "2026-04-27T10:00:00Z",
                        "temperature_2m": 24.5,
                        "relative_humidity_2m": 78
                    },
                    "interpretation": "Día despejado"
                }""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val api: WeatherApi = KtorWeatherApi(HttpClientFactory.create(engine = mockEngine))

        val result = api.getCurrentWeather(lat = 14.9726, lon = -89.5301)

        assertEquals(24.5, result.current?.temperature2m)
        assertEquals(78, result.current?.relativeHumidity2m)
        assertEquals("Día despejado", result.interpretation)
        assertEquals("2026-04-27T10:00:00Z", result.current?.time)
    }

    @Test
    fun `getCurrentWeather handles nullable fields`() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = "{}",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val api: WeatherApi = KtorWeatherApi(HttpClientFactory.create(engine = mockEngine))

        val result = api.getCurrentWeather(lat = 0.0, lon = 0.0)

        assertNull(result.current)
        assertNull(result.interpretation)
    }

    @Test
    fun `getCurrentWeather returns ServerError on 500`() = runTest {
        val mockEngine = MockEngine {
            respondError(status = HttpStatusCode.InternalServerError)
        }

        val api: WeatherApi = KtorWeatherApi(HttpClientFactory.create(engine = mockEngine))

        val error = runCatching { api.getCurrentWeather(0.0, 0.0) }.exceptionOrNull()

        assertIs<ApiError.ServerError>(error)
    }

    @Test
    fun `getCurrentWeather returns NetworkError on engine failure`() = runTest {
        val mockEngine = MockEngine {
            throw RuntimeException("No connection")
        }

        val api: WeatherApi = KtorWeatherApi(HttpClientFactory.create(engine = mockEngine))

        val error = runCatching { api.getCurrentWeather(0.0, 0.0) }.exceptionOrNull()

        assertIs<ApiError.NetworkError>(error)
    }
}
