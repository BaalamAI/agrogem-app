package com.agrogem.app.data.weather.domain

import com.agrogem.app.data.shared.domain.LatLng
import com.agrogem.app.data.weather.api.WeatherApi
import com.agrogem.app.data.weather.api.WeatherResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WeatherRepositoryTest {

    @Test
    fun `getCurrentWeather maps all fields on happy path`() = runTest {
        val fakeApi = FakeWeatherApi(
            response = WeatherResponse(
                temperatureCelsius = 24.5,
                humidityPercentage = 78.0,
                cloudCoverPercentage = 65.0,
                uvIndex = 3.0,
                description = "Día despejado",
                dateTime = "2026-04-27T10:00:00Z",
            )
        )
        val repo = WeatherRepositoryImpl(api = fakeApi)

        val result = repo.getCurrentWeather(LatLng(14.9726, -89.5301))

        assertTrue(result.isSuccess)
        val weather = result.getOrNull()
        assertEquals("24.5°C", weather?.temperatureCelsius)
        assertEquals("78%", weather?.humidity)
        assertEquals("65%", weather?.cloudCover)
        assertEquals("3.0", weather?.uvIndex)
        assertEquals("Día despejado", weather?.description)
        assertEquals("2026-04-27T10:00:00Z", weather?.dateLabel)
    }

    @Test
    fun `getCurrentWeather maps null temperature to default`() = runTest {
        val fakeApi = FakeWeatherApi(
            response = WeatherResponse(
                temperatureCelsius = null,
                humidityPercentage = 78.0,
                cloudCoverPercentage = 65.0,
                uvIndex = 3.0,
                description = "Día despejado",
                dateTime = "2026-04-27T10:00:00Z",
            )
        )
        val repo = WeatherRepositoryImpl(api = fakeApi)

        val result = repo.getCurrentWeather(LatLng(0.0, 0.0))

        assertTrue(result.isSuccess)
        val weather = result.getOrNull()
        assertEquals("--", weather?.temperatureCelsius)
    }

    @Test
    fun `getCurrentWeather maps null humidity to default`() = runTest {
        val fakeApi = FakeWeatherApi(
            response = WeatherResponse(
                temperatureCelsius = 24.5,
                humidityPercentage = null,
                cloudCoverPercentage = 65.0,
                uvIndex = 3.0,
                description = "Día despejado",
                dateTime = "2026-04-27T10:00:00Z",
            )
        )
        val repo = WeatherRepositoryImpl(api = fakeApi)

        val result = repo.getCurrentWeather(LatLng(0.0, 0.0))

        assertTrue(result.isSuccess)
        val weather = result.getOrNull()
        assertEquals("--", weather?.humidity)
    }

    @Test
    fun `getCurrentWeather maps all nulls to defaults`() = runTest {
        val fakeApi = FakeWeatherApi(response = WeatherResponse())
        val repo = WeatherRepositoryImpl(api = fakeApi)

        val result = repo.getCurrentWeather(LatLng(0.0, 0.0))

        assertTrue(result.isSuccess)
        val weather = result.getOrNull()
        assertEquals("--", weather?.temperatureCelsius)
        assertEquals("--", weather?.humidity)
        assertEquals("--", weather?.cloudCover)
        assertEquals("--", weather?.uvIndex)
        assertEquals("--", weather?.description)
        assertEquals("--", weather?.dateLabel)
    }

    private class FakeWeatherApi(
        private val response: WeatherResponse = WeatherResponse()
    ) : WeatherApi {
        override suspend fun getCurrentWeather(lat: Double, lng: Double): WeatherResponse = response
    }
}
