package com.agrogem.app.data.weather.domain

import com.agrogem.app.data.shared.domain.LatLng
import com.agrogem.app.data.weather.api.CurrentWeatherDto
import com.agrogem.app.data.weather.api.DailyWeatherDto
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
                current = CurrentWeatherDto(
                    temperature2m = 24.5,
                    relativeHumidity2m = 78,
                    precipitation = 1.2,
                    windSpeed10m = 9.0,
                    weatherCode = 1,
                    time = "2026-04-27T10:00:00Z",
                ),
                daily = DailyWeatherDto(
                    temperature2mMax = listOf(29.1),
                    temperature2mMin = listOf(19.3),
                    uvIndexMax = listOf(5.4),
                ),
                interpretation = "Día despejado",
            )
        )
        val repo = WeatherRepositoryImpl(api = fakeApi, cacheStore = FakeWeatherCacheStore())

        val result = repo.getCurrentWeather(LatLng(14.9726, -89.5301))

        assertTrue(result.isSuccess)
        val weather = result.getOrNull()
        assertEquals("24°C", weather?.temperatureCelsius)
        assertEquals("78%", weather?.humidity)
        assertEquals("1.2 mm", weather?.precipitation)
        assertEquals("9 km/h", weather?.windSpeed)
        assertEquals("29°/19°", weather?.maxMin)
        assertEquals("5.4", weather?.uvIndex)
        assertEquals("Parcialmente nublado", weather?.description)
        assertEquals("2026-04-27T10:00:00Z", weather?.dateLabel)
    }

    @Test
    fun `getCurrentWeather maps null temperature to default`() = runTest {
        val fakeApi = FakeWeatherApi(
            response = WeatherResponse(
                current = CurrentWeatherDto(
                    temperature2m = null,
                    relativeHumidity2m = 78,
                    time = "2026-04-27T10:00:00Z",
                ),
                interpretation = "Día despejado",
            )
        )
        val repo = WeatherRepositoryImpl(api = fakeApi, cacheStore = FakeWeatherCacheStore())

        val result = repo.getCurrentWeather(LatLng(0.0, 0.0))

        assertTrue(result.isSuccess)
        val weather = result.getOrNull()
        assertEquals("--", weather?.temperatureCelsius)
    }

    @Test
    fun `getCurrentWeather maps null humidity to default`() = runTest {
        val fakeApi = FakeWeatherApi(
            response = WeatherResponse(
                current = CurrentWeatherDto(
                    temperature2m = 24.5,
                    relativeHumidity2m = null,
                    time = "2026-04-27T10:00:00Z",
                ),
                interpretation = "Día despejado",
            )
        )
        val repo = WeatherRepositoryImpl(api = fakeApi, cacheStore = FakeWeatherCacheStore())

        val result = repo.getCurrentWeather(LatLng(0.0, 0.0))

        assertTrue(result.isSuccess)
        val weather = result.getOrNull()
        assertEquals("--", weather?.humidity)
    }

    @Test
    fun `getCurrentWeather maps all nulls to defaults`() = runTest {
        val fakeApi = FakeWeatherApi(response = WeatherResponse())
        val repo = WeatherRepositoryImpl(api = fakeApi, cacheStore = FakeWeatherCacheStore())

        val result = repo.getCurrentWeather(LatLng(0.0, 0.0))

        assertTrue(result.isSuccess)
        val weather = result.getOrNull()
        assertEquals("--", weather?.temperatureCelsius)
        assertEquals("--", weather?.humidity)
        assertEquals("--", weather?.precipitation)
        assertEquals("--", weather?.windSpeed)
        assertEquals("--", weather?.maxMin)
        assertEquals("--", weather?.uvIndex)
        assertEquals("--", weather?.description)
        assertEquals("--", weather?.dateLabel)
    }

    private class FakeWeatherApi(
        private val response: WeatherResponse = WeatherResponse()
    ) : WeatherApi {
        override suspend fun getCurrentWeather(lat: Double, lon: Double): WeatherResponse = response
    }

    private class FakeWeatherCacheStore : WeatherCacheStore {
        private var entry: WeatherCacheEntry? = null

        override suspend fun read(): WeatherCacheEntry? = entry

        override suspend fun write(entry: WeatherCacheEntry) {
            this.entry = entry
        }
    }
}
