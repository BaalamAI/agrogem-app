package com.agrogem.app.data.soil.domain

import com.agrogem.app.data.shared.domain.LatLng
import com.agrogem.app.data.soil.api.HorizonDto
import com.agrogem.app.data.soil.api.SoilApi
import com.agrogem.app.data.soil.api.SoilResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SoilRepositoryTest {

    @Test
    fun `getSoil maps full profile with summary`() = runTest {
        val fakeApi = FakeSoilApi(
            response = SoilResponse(
                lat = 14.9726,
                lon = -89.5301,
                dominantTexture = "Clay loam",
                horizons = listOf(
                    HorizonDto(depth = "0-5cm", ph = 6.2, textureClass = "Clay loam", socGPerKg = 12.5)
                )
            )
        )
        val repo = SoilRepositoryImpl(api = fakeApi)

        val result = repo.getSoil(LatLng(14.9726, -89.5301))

        assertTrue(result.isSuccess)
        val profile = result.getOrNull()!!
        assertEquals(14.9726, profile.lat)
        assertEquals(-89.5301, profile.lon)
        assertEquals("Clay loam", profile.dominantTexture)
        assertEquals(1, profile.domainHorizons.size)
        assertEquals("Clay loam", profile.summary.dominantTexture)
        assertEquals(6.2, profile.summary.topHorizonPh)
    }

    @Test
    fun `getSoil preserves missing summary values as null`() = runTest {
        val fakeApi = FakeSoilApi(
            response = SoilResponse(
                lat = null,
                lon = null,
                dominantTexture = null,
                horizons = listOf(
                    HorizonDto(depth = null, ph = null, textureClass = null, socGPerKg = null)
                )
            )
        )
        val repo = SoilRepositoryImpl(api = fakeApi)

        val result = repo.getSoil(LatLng(0.0, 0.0))

        assertTrue(result.isSuccess)
        val profile = result.getOrNull()!!
        assertEquals(0.0, profile.lat)
        assertEquals(0.0, profile.lon)
        assertEquals("", profile.dominantTexture)
        val horizon = profile.domainHorizons[0]
        assertEquals("", horizon.depth)
        assertNull(horizon.ph)
        assertEquals("", horizon.textureClass)
        assertEquals(0.0, horizon.socGPerKg)
        assertNull(profile.summary.dominantTexture)
        assertNull(profile.summary.topHorizonPh)
    }

    @Test
    fun `getSoil maps null horizons to emptyList`() = runTest {
        val fakeApi = FakeSoilApi(
            response = SoilResponse(
                lat = 14.9726,
                lon = -89.5301,
                dominantTexture = null,
                horizons = null,
            )
        )
        val repo = SoilRepositoryImpl(api = fakeApi)

        val result = repo.getSoil(LatLng(14.9726, -89.5301))

        assertTrue(result.isSuccess)
        val profile = result.getOrNull()!!
        assertEquals(0, profile.domainHorizons.size)
        assertNull(profile.summary.dominantTexture)
        assertNull(profile.summary.topHorizonPh)
    }

    @Test
    fun `getSoil maps empty horizons to emptyList with default summary`() = runTest {
        val fakeApi = FakeSoilApi(
            response = SoilResponse(
                lat = 14.9726,
                lon = -89.5301,
                dominantTexture = null,
                horizons = emptyList(),
            )
        )
        val repo = SoilRepositoryImpl(api = fakeApi)

        val result = repo.getSoil(LatLng(14.9726, -89.5301))

        assertTrue(result.isSuccess)
        val profile = result.getOrNull()!!
        assertEquals(0, profile.domainHorizons.size)
        assertNull(profile.summary.dominantTexture)
        assertNull(profile.summary.topHorizonPh)
    }

    @Test
    fun `getSoil derives dominantTexture from first horizon when response field null`() = runTest {
        val fakeApi = FakeSoilApi(
            response = SoilResponse(
                lat = 14.9726,
                lon = -89.5301,
                dominantTexture = null,
                horizons = listOf(
                    HorizonDto(depth = "0-5cm", ph = 6.5, textureClass = "Sandy loam", socGPerKg = 8.0)
                )
            )
        )
        val repo = SoilRepositoryImpl(api = fakeApi)

        val result = repo.getSoil(LatLng(14.9726, -89.5301))

        assertTrue(result.isSuccess)
        val profile = result.getOrNull()!!
        assertEquals("Sandy loam", profile.summary.dominantTexture)
    }

    @Test
    fun `getSoil returns failure when api throws`() = runTest {
        val fakeApi = FakeSoilApi(shouldThrow = true)
        val repo = SoilRepositoryImpl(api = fakeApi)

        val result = repo.getSoil(LatLng(0.0, 0.0))

        assertTrue(result.isFailure)
    }

    private class FakeSoilApi(
        private val response: SoilResponse = SoilResponse(),
        private val shouldThrow: Boolean = false,
    ) : SoilApi {
        override suspend fun getSoil(lat: Double, lon: Double): SoilResponse {
            if (shouldThrow) throw RuntimeException("API error")
            return response
        }
    }
}
