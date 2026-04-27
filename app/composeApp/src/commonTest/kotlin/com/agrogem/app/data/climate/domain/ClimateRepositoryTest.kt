package com.agrogem.app.data.climate.domain

import com.agrogem.app.data.climate.api.ClimateApi
import com.agrogem.app.data.climate.api.ClimateDataPointDto
import com.agrogem.app.data.climate.api.ClimateHistoryResponse
import com.agrogem.app.data.shared.domain.LatLng
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ClimateRepositoryTest {

    @Test
    fun `getClimateHistory with latLng only delegates with default query`() = runTest {
        val fakeApi = FakeClimateApi(
            response = ClimateHistoryResponse(
                lat = 14.9726,
                lon = -89.5301,
                granularity = "monthly",
                series = emptyList(),
            )
        )
        val repo = ClimateRepositoryImpl(api = fakeApi)

        val result = repo.getClimateHistory(LatLng(14.9726, -89.5301))

        assertTrue(result.isSuccess)
        assertNotNull(fakeApi.capturedStart)
        assertNotNull(fakeApi.capturedEnd)
        assertEquals("monthly", fakeApi.capturedGranularity)
    }

    @Test
    fun `getClimateHistory with latLng only propagates api error`() = runTest {
        val fakeApi = FakeClimateApi(shouldThrow = true)
        val repo = ClimateRepositoryImpl(api = fakeApi)

        val result = repo.getClimateHistory(LatLng(0.0, 0.0))

        assertTrue(result.isFailure)
    }

    @Test
    fun `getClimateHistory maps full response`() = runTest {
        val fakeApi = FakeClimateApi(
            response = ClimateHistoryResponse(
                lat = 14.9726,
                lon = -89.5301,
                granularity = "monthly",
                series = listOf(
                    ClimateDataPointDto(
                        date = "2024-01",
                        t2m = 22.5,
                        t2mMax = 28.0,
                        t2mMin = 17.0,
                        precipitationMm = 45.2,
                        rhPct = 78.0,
                        solarMjM2 = 15.3,
                    )
                )
            )
        )
        val repo = ClimateRepositoryImpl(api = fakeApi)

        val result = repo.getClimateHistory(
            LatLng(14.9726, -89.5301),
            start = "2024-01-01",
            end = "2024-12-31",
            granularity = "monthly",
        )

        assertTrue(result.isSuccess)
        val history = result.getOrNull()!!
        assertEquals(14.9726, history.lat)
        assertEquals(-89.5301, history.lon)
        assertEquals("monthly", history.granularity)
        assertEquals(1, history.domainSeries.size)
        val point = history.domainSeries[0]
        assertEquals("2024-01", point.date)
        assertEquals(22.5, point.t2m)
        assertEquals(28.0, point.t2mMax)
        assertEquals(17.0, point.t2mMin)
        assertEquals(45.2, point.precipitationMm)
        assertEquals(78.0, point.rhPct)
        assertEquals(15.3, point.solarMjM2)
    }

    @Test
    fun `getClimateHistory maps empty series`() = runTest {
        val fakeApi = FakeClimateApi(
            response = ClimateHistoryResponse(
                lat = 14.9726,
                lon = -89.5301,
                granularity = "monthly",
                series = emptyList(),
            )
        )
        val repo = ClimateRepositoryImpl(api = fakeApi)

        val result = repo.getClimateHistory(
            LatLng(14.9726, -89.5301),
            start = "2024-01-01",
            end = "2024-12-31",
            granularity = "monthly",
        )

        assertTrue(result.isSuccess)
        val history = result.getOrNull()!!
        assertEquals(0, history.domainSeries.size)
    }

    @Test
    fun `getClimateHistory maps null field defaults`() = runTest {
        val fakeApi = FakeClimateApi(
            response = ClimateHistoryResponse(
                lat = null,
                lon = null,
                granularity = null,
                series = listOf(
                    ClimateDataPointDto(
                        date = null,
                        t2m = null,
                        t2mMax = null,
                        t2mMin = null,
                        precipitationMm = null,
                        rhPct = null,
                        solarMjM2 = null,
                    )
                )
            )
        )
        val repo = ClimateRepositoryImpl(api = fakeApi)

        val result = repo.getClimateHistory(
            LatLng(0.0, 0.0),
            start = "2024-01-01",
            end = "2024-12-31",
            granularity = "monthly",
        )

        assertTrue(result.isSuccess)
        val history = result.getOrNull()!!
        assertEquals(0.0, history.lat)
        assertEquals(0.0, history.lon)
        assertEquals("", history.granularity)
        val point = history.domainSeries[0]
        assertEquals("", point.date)
        assertEquals(0.0, point.t2m)
        assertEquals(0.0, point.t2mMax)
        assertEquals(0.0, point.t2mMin)
        assertEquals(0.0, point.precipitationMm)
        assertEquals(0.0, point.rhPct)
        assertEquals(0.0, point.solarMjM2)
    }

    @Test
    fun `getClimateHistory propagates api error`() = runTest {
        val fakeApi = FakeClimateApi(shouldThrow = true)
        val repo = ClimateRepositoryImpl(api = fakeApi)

        val result = repo.getClimateHistory(
            LatLng(0.0, 0.0),
            start = "2024-01-01",
            end = "2024-12-31",
            granularity = "monthly",
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `getClimateHistory passes monthly date strings`() = runTest {
        val fakeApi = FakeClimateApi(
            response = ClimateHistoryResponse(
                lat = 14.9726,
                lon = -89.5301,
                granularity = "monthly",
                series = listOf(
                    ClimateDataPointDto(date = "2024-01", t2m = 20.0)
                )
            )
        )
        val repo = ClimateRepositoryImpl(api = fakeApi)

        val result = repo.getClimateHistory(
            LatLng(14.9726, -89.5301),
            start = "2024-01-01",
            end = "2024-12-31",
            granularity = "monthly",
        )

        assertTrue(result.isSuccess)
        assertEquals("2024-01", result.getOrNull()!!.domainSeries[0].date)
        assertEquals("2024-01-01", fakeApi.capturedStart)
        assertEquals("2024-12-31", fakeApi.capturedEnd)
        assertEquals("monthly", fakeApi.capturedGranularity)
    }

    @Test
    fun `getClimateHistory coerces daily to monthly when range exceeds 366 days`() = runTest {
        val fakeApi = FakeClimateApi(
            response = ClimateHistoryResponse(
                lat = 14.9726,
                lon = -89.5301,
                granularity = "monthly",
                series = emptyList(),
            )
        )
        val repo = ClimateRepositoryImpl(api = fakeApi)

        repo.getClimateHistory(
            LatLng(14.9726, -89.5301),
            start = "2023-01-01",
            end = "2024-12-31",
            granularity = "daily",
        )

        assertEquals("monthly", fakeApi.capturedGranularity)
    }

    @Test
    fun `exceedsDailyLimit returns true for ranges over 366 days`() {
        assertTrue(exceedsDailyLimit("2023-01-01", "2024-12-31"))
        assertTrue(exceedsDailyLimit("2024-01-01", "2025-01-02"))
    }

    @Test
    fun `exceedsDailyLimit returns false for ranges within 366 days`() {
        assertFalse(exceedsDailyLimit("2024-01-01", "2024-12-31"))
        assertFalse(exceedsDailyLimit("2024-01-01", "2025-01-01"))
    }

    private class FakeClimateApi(
        private val response: ClimateHistoryResponse = ClimateHistoryResponse(),
        private val shouldThrow: Boolean = false,
    ) : ClimateApi {
        var capturedStart: String? = null
        var capturedEnd: String? = null
        var capturedGranularity: String? = null

        override suspend fun getClimateHistory(
            lat: Double,
            lon: Double,
            start: String,
            end: String,
            granularity: String,
        ): ClimateHistoryResponse {
            if (shouldThrow) throw RuntimeException("API error")
            capturedStart = start
            capturedEnd = end
            capturedGranularity = granularity
            return response
        }
    }
}
