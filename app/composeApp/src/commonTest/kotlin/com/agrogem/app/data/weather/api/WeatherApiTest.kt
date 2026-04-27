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
    fun `getCurrentWeather returns full response with lat lng params`() = runTest {
        val mockEngine = MockEngine { request ->
            assertTrue(request.url.encodedPath.endsWith("/weather/current"))
            assertEquals("14.9726", request.url.parameters["lat"])
            assertEquals("-89.5301", request.url.parameters["lng"])
            respond(
                content = """{
                    "temperature_Celsius": 24.5,
                    "humidity_Percentage": 78.0,
                    "cloudCover_Percentage": 65.0,
                    "uvIndex": 3.0,
                    "description": "Día despejado",
                    "dateTime": "2026-04-27T10:00:00Z"
                }""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val api: WeatherApi = KtorWeatherApi(HttpClientFactory.create(engine = mockEngine))

        val result = api.getCurrentWeather(lat = 14.9726, lng = -89.5301)

        assertEquals(24.5, result.temperatureCelsius)
        assertEquals(78.0, result.humidityPercentage)
        assertEquals(65.0, result.cloudCoverPercentage)
        assertEquals(3.0, result.uvIndex)
        assertEquals("Día despejado", result.description)
        assertEquals("2026-04-27T10:00:00Z", result.dateTime)
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

        val result = api.getCurrentWeather(lat = 0.0, lng = 0.0)

        assertNull(result.temperatureCelsius)
        assertNull(result.humidityPercentage)
        assertNull(result.cloudCoverPercentage)
        assertNull(result.uvIndex)
        assertNull(result.description)
        assertNull(result.dateTime)
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
