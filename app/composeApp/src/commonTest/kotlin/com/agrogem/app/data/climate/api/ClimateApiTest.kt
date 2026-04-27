package com.agrogem.app.data.climate.api

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

class ClimateApiTest {

    @Test
    fun `getClimateHistory returns full response with query params`() = runTest {
        val mockEngine = MockEngine { request ->
            assertTrue(request.url.encodedPath.endsWith("/climate/history"))
            assertEquals("14.9726", request.url.parameters["lat"])
            assertEquals("-89.5301", request.url.parameters["lon"])
            assertEquals("2024-01-01", request.url.parameters["start"])
            assertEquals("2024-12-31", request.url.parameters["end"])
            assertEquals("monthly", request.url.parameters["granularity"])
            respond(
                content = """{
                    "lat": 14.9726,
                    "lon": -89.5301,
                    "granularity": "monthly",
                    "series": [
                        {"date":"2024-01","t2m":22.5,"t2m_max":28.0,"t2m_min":17.0,"precipitation_mm":45.2,"rh_pct":78.0,"solar_mj_m2":15.3}
                    ]
                }""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val api: ClimateApi = KtorClimateApi(HttpClientFactory.create(engine = mockEngine, socketTimeoutMs = 20_000))

        val result = api.getClimateHistory(
            lat = 14.9726,
            lon = -89.5301,
            start = "2024-01-01",
            end = "2024-12-31",
            granularity = "monthly",
        )

        assertEquals(14.9726, result.lat)
        assertEquals(-89.5301, result.lon)
        assertEquals("monthly", result.granularity)
        assertEquals(1, result.series?.size)
        assertEquals("2024-01", result.series?.get(0)?.date)
        assertEquals(22.5, result.series?.get(0)?.t2m)
        assertEquals(28.0, result.series?.get(0)?.t2mMax)
        assertEquals(17.0, result.series?.get(0)?.t2mMin)
        assertEquals(45.2, result.series?.get(0)?.precipitationMm)
        assertEquals(78.0, result.series?.get(0)?.rhPct)
        assertEquals(15.3, result.series?.get(0)?.solarMjM2)
    }

    @Test
    fun `getClimateHistory decodes null series safely`() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = """{"lat":14.9726,"lon":-89.5301,"granularity":"monthly","series":null}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val api: ClimateApi = KtorClimateApi(HttpClientFactory.create(engine = mockEngine, socketTimeoutMs = 20_000))

        val result = api.getClimateHistory(14.9726, -89.5301, "2024-01-01", "2024-12-31", "monthly")

        assertEquals(null, result.series)
    }

    @Test
    fun `getClimateHistory returns NotFound on 404`() = runTest {
        val mockEngine = MockEngine {
            respondError(status = HttpStatusCode.NotFound)
        }

        val api: ClimateApi = KtorClimateApi(HttpClientFactory.create(engine = mockEngine, socketTimeoutMs = 20_000))

        val error = runCatching { api.getClimateHistory(0.0, 0.0, "2024-01-01", "2024-12-31", "monthly") }.exceptionOrNull()

        assertIs<ApiError.NotFound>(error)
    }

    @Test
    fun `getClimateHistory returns ServerError on 500`() = runTest {
        val mockEngine = MockEngine {
            respondError(status = HttpStatusCode.InternalServerError)
        }

        val api: ClimateApi = KtorClimateApi(HttpClientFactory.create(engine = mockEngine, socketTimeoutMs = 20_000))

        val error = runCatching { api.getClimateHistory(0.0, 0.0, "2024-01-01", "2024-12-31", "monthly") }.exceptionOrNull()

        assertIs<ApiError.ServerError>(error)
    }

    @Test
    fun `getClimateHistory handles malformed json`() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = "{invalid json",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val api: ClimateApi = KtorClimateApi(HttpClientFactory.create(engine = mockEngine, socketTimeoutMs = 20_000))

        val error = runCatching { api.getClimateHistory(0.0, 0.0, "2024-01-01", "2024-12-31", "monthly") }.exceptionOrNull()

        assertIs<ApiError.NetworkError>(error)
    }

    @Test
    fun `getClimateHistory returns NetworkError on engine failure`() = runTest {
        val mockEngine = MockEngine {
            throw RuntimeException("No connection")
        }

        val api: ClimateApi = KtorClimateApi(HttpClientFactory.create(engine = mockEngine, socketTimeoutMs = 20_000))

        val error = runCatching { api.getClimateHistory(0.0, 0.0, "2024-01-01", "2024-12-31", "monthly") }.exceptionOrNull()

        assertIs<ApiError.NetworkError>(error)
    }
}
