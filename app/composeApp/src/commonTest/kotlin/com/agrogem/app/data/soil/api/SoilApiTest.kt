package com.agrogem.app.data.soil.api

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

class SoilApiTest {

    @Test
    fun `getSoil returns full response with lat lon params`() = runTest {
        val mockEngine = MockEngine { request ->
            assertTrue(request.url.encodedPath.endsWith("/soil"))
            assertEquals("14.9726", request.url.parameters["lat"])
            assertEquals("-89.5301", request.url.parameters["lon"])
            respond(
                content = """{
                    "lat": 14.9726,
                    "lon": -89.5301,
                    "dominant_texture": "Clay loam",
                    "horizons": [
                        {"depth":"0-5cm","ph":6.2,"texture_class":"Clay loam","soc_g_per_kg":12.5}
                    ]
                }""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val api: SoilApi = KtorSoilApi(HttpClientFactory.create(engine = mockEngine))

        val result = api.getSoil(lat = 14.9726, lon = -89.5301)

        assertEquals(14.9726, result.lat)
        assertEquals(-89.5301, result.lon)
        assertEquals("Clay loam", result.dominantTexture)
        assertEquals(1, result.horizons?.size)
        assertEquals("0-5cm", result.horizons?.get(0)?.depth)
        assertEquals(6.2, result.horizons?.get(0)?.ph)
        assertEquals("Clay loam", result.horizons?.get(0)?.textureClass)
        assertEquals(12.5, result.horizons?.get(0)?.socGPerKg)
    }

    @Test
    fun `getSoil decodes null horizons safely`() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = """{"lat":14.9726,"lon":-89.5301,"horizons":null}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val api: SoilApi = KtorSoilApi(HttpClientFactory.create(engine = mockEngine))

        val result = api.getSoil(lat = 14.9726, lon = -89.5301)

        assertNull(result.horizons)
        assertNull(result.dominantTexture)
    }

    @Test
    fun `getSoil returns ServerError on 500`() = runTest {
        val mockEngine = MockEngine {
            respondError(status = HttpStatusCode.InternalServerError)
        }

        val api: SoilApi = KtorSoilApi(HttpClientFactory.create(engine = mockEngine))

        val error = runCatching { api.getSoil(0.0, 0.0) }.exceptionOrNull()

        assertIs<ApiError.ServerError>(error)
    }

    @Test
    fun `getSoil returns NetworkError on engine failure`() = runTest {
        val mockEngine = MockEngine {
            throw RuntimeException("No connection")
        }

        val api: SoilApi = KtorSoilApi(HttpClientFactory.create(engine = mockEngine))

        val error = runCatching { api.getSoil(0.0, 0.0) }.exceptionOrNull()

        assertIs<ApiError.NetworkError>(error)
    }

    @Test
    fun `getSoil returns NotFound on 404`() = runTest {
        val mockEngine = MockEngine {
            respondError(status = HttpStatusCode.NotFound)
        }

        val api: SoilApi = KtorSoilApi(HttpClientFactory.create(engine = mockEngine))

        val error = runCatching { api.getSoil(0.0, 0.0) }.exceptionOrNull()

        assertIs<ApiError.NotFound>(error)
    }

    @Test
    fun `getSoil handles malformed json`() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = "{invalid json",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val api: SoilApi = KtorSoilApi(HttpClientFactory.create(engine = mockEngine))

        val error = runCatching { api.getSoil(0.0, 0.0) }.exceptionOrNull()

        assertIs<ApiError.NetworkError>(error)
    }
}
