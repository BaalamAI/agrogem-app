package com.agrogem.app.data.geolocation.api

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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GeolocationApiTest {

    @Test
    fun `geocode returns single hit`() = runTest {
        val mockEngine = MockEngine { request ->
            assertTrue(request.url.encodedPath.endsWith("/geocode"))
            assertEquals("Zacapa, Guatemala", request.url.parameters["q"])
            respond(
                content = """{
                    "display_name":"Zacapa, Guatemala",
                    "lat":14.9726,
                    "lon":-89.5301,
                    "municipality":"Zacapa",
                    "state":"Zacapa Department",
                    "country_code":"gt",
                    "interpretation":"Ubicación encontrada"
                }""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val api: GeolocationApi = KtorGeolocationApi(HttpClientFactory.create(engine = mockEngine))

        val result = api.geocode("Zacapa, Guatemala")

        assertNotNull(result)
        assertEquals("Zacapa, Guatemala", result.name)
        assertEquals(14.9726, result.lat)
        assertEquals(-89.5301, result.lng)
        assertEquals("Zacapa", result.municipality)
        assertEquals("Ubicación encontrada", result.interpretation)
    }

    @Test
    fun `geocode returns null on 404`() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = """{"detail":"No match found for query."}""",
                status = HttpStatusCode.NotFound,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val api: GeolocationApi = KtorGeolocationApi(HttpClientFactory.create(engine = mockEngine))

        val result = api.geocode("xkqzz_no_such_place")

        assertNull(result)
    }

    @Test
    fun `reverseGeocode returns location info with name and coords`() = runTest {
        val mockEngine = MockEngine { request ->
            assertTrue(request.url.encodedPath.endsWith("/geocode/reverse"))
            assertEquals("14.9726", request.url.parameters["lat"])
            assertEquals("-89.5301", request.url.parameters["lon"])
            respond(
                content = """{
                    "display_name":"Zacapa, Guatemala",
                    "lat":14.9726,
                    "lon":-89.5301,
                    "municipality":"Zacapa",
                    "state":"Zacapa Department",
                    "country_code":"gt"
                }""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val api: GeolocationApi = KtorGeolocationApi(HttpClientFactory.create(engine = mockEngine))

        val result = api.reverseGeocode(lat = 14.9726, lng = -89.5301)

        assertEquals("Zacapa, Guatemala", result.displayName)
        assertEquals(14.9726, result.lat)
        assertEquals(-89.5301, result.lng)
        assertEquals("Zacapa", result.municipality)
        assertEquals("Zacapa Department", result.state)
        assertEquals("gt", result.country)
    }

    @Test
    fun `elevation returns meters`() = runTest {
        val mockEngine = MockEngine { request ->
            assertTrue(request.url.encodedPath.endsWith("/elevation"))
            assertEquals("14.9726", request.url.parameters["lat"])
            assertEquals("-89.5301", request.url.parameters["lon"])
            respond(
                content = """{"elevation_m":230.5}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val api: GeolocationApi = KtorGeolocationApi(HttpClientFactory.create(engine = mockEngine))

        val result = api.elevation(lat = 14.9726, lng = -89.5301)

        assertEquals(230.5, result.elevationMeters)
    }

    @Test
    fun `geocode returns ServerError on 500`() = runTest {
        val mockEngine = MockEngine {
            respondError(status = HttpStatusCode.InternalServerError)
        }

        val api: GeolocationApi = KtorGeolocationApi(HttpClientFactory.create(engine = mockEngine))

        val error = runCatching { api.geocode("query") }.exceptionOrNull()

        assertIs<ApiError.ServerError>(error)
    }

    @Test
    fun `reverseGeocode returns NetworkError on engine failure`() = runTest {
        val mockEngine = MockEngine {
            throw RuntimeException("No connection")
        }

        val api: GeolocationApi = KtorGeolocationApi(HttpClientFactory.create(engine = mockEngine))

        val error = runCatching { api.reverseGeocode(0.0, 0.0) }.exceptionOrNull()

        assertIs<ApiError.NetworkError>(error)
    }

    @Test
    fun `elevation returns ServerError on 500`() = runTest {
        val mockEngine = MockEngine {
            respondError(status = HttpStatusCode.InternalServerError)
        }

        val api: GeolocationApi = KtorGeolocationApi(HttpClientFactory.create(engine = mockEngine))

        val error = runCatching { api.elevation(0.0, 0.0) }.exceptionOrNull()

        assertIs<ApiError.ServerError>(error)
    }
}
