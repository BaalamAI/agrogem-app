package com.agrogem.app.ui.screens.home

import com.agrogem.app.data.geolocation.domain.GeolocationRepository
import com.agrogem.app.data.geolocation.domain.LocationDisplay
import com.agrogem.app.data.geolocation.domain.ResolvedLocation
import com.agrogem.app.data.shared.domain.LatLng
import com.agrogem.app.data.soil.domain.SoilProfile
import com.agrogem.app.data.soil.domain.SoilRepository
import com.agrogem.app.data.weather.domain.CurrentWeather
import com.agrogem.app.data.weather.domain.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init transitions Loading to Data when location and weather succeed`() = runTest(testDispatcher) {
        val location = ResolvedLocation(
            coordinates = LatLng(14.9726, -89.5301),
            display = LocationDisplay(
                primary = "Zacapa, Guatemala",
                municipality = "Zacapa",
                state = "Zacapa Department",
                country = "Guatemala",
            ),
            elevationMeters = 230.5,
        )
        val weather = CurrentWeather(
            temperatureCelsius = "24.5°C",
            humidity = "78%",
            cloudCover = "65%",
            uvIndex = "3.0",
            description = "Día despejado",
            locationName = "",
            dateLabel = "2026-04-27",
        )
        val geoRepo = FakeGeolocationRepository(location)
        val weatherRepo = FakeWeatherRepository(Result.success(weather))
        val soilProfile = SoilProfile(
            lat = 14.9726,
            lon = -89.5301,
            dominantTexture = "Clay loam",
            domainHorizons = emptyList(),
        )
        val soilRepo = FakeSoilRepository(Result.success(soilProfile))
        val viewModel = HomeViewModel(geoRepo, weatherRepo, soilRepo)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<HomeUiState.Data>(state)
        assertEquals("Zacapa, Guatemala", state.locationInfo.display.primary)
        assertEquals("24.5°C", state.weather.temperatureCelsius)
        assertEquals("78%", state.metrics.humidity)
        assertEquals("Clay loam", state.soilSummary?.dominantTexture)
        assertEquals(0.0, state.soilSummary?.topHorizonPh)
    }

    @Test
    fun `init transitions Loading to LocationMissing when store empty`() = runTest(testDispatcher) {
        val geoRepo = FakeGeolocationRepository(null)
        val weatherRepo = FakeWeatherRepository(Result.failure(Exception("no-op")))
        val soilRepo = FakeSoilRepository(Result.failure(Exception("no-op")))
        val viewModel = HomeViewModel(geoRepo, weatherRepo, soilRepo)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<HomeUiState.LocationMissing>(state)
    }

    @Test
    fun `init transitions Loading to Error when weather fails`() = runTest(testDispatcher) {
        val location = ResolvedLocation(
            coordinates = LatLng(14.9726, -89.5301),
            display = LocationDisplay(
                primary = "Zacapa, Guatemala",
                municipality = "Zacapa",
                state = "Zacapa Department",
                country = "Guatemala",
            ),
            elevationMeters = null,
        )
        val geoRepo = FakeGeolocationRepository(location)
        val weatherRepo = FakeWeatherRepository(Result.failure(Exception("network error")))
        val soilProfile = SoilProfile(
            lat = 14.9726,
            lon = -89.5301,
            dominantTexture = "Clay loam",
            domainHorizons = emptyList(),
        )
        val soilRepo = FakeSoilRepository(Result.success(soilProfile))
        val viewModel = HomeViewModel(geoRepo, weatherRepo, soilRepo)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<HomeUiState.Error>(state)
        assertTrue(state.retryable)
    }

    @Test
    fun `retry resets to Loading then Data`() = runTest(testDispatcher) {
        val location = ResolvedLocation(
            coordinates = LatLng(14.9726, -89.5301),
            display = LocationDisplay(
                primary = "Zacapa, Guatemala",
                municipality = "Zacapa",
                state = "Zacapa Department",
                country = "Guatemala",
            ),
            elevationMeters = null,
        )
        val weather = CurrentWeather(
            temperatureCelsius = "24.5°C",
            humidity = "78%",
            cloudCover = "65%",
            uvIndex = "3.0",
            description = "Día despejado",
            locationName = "",
            dateLabel = "2026-04-27",
        )
        val geoRepo = FakeGeolocationRepository(location)
        val weatherRepo = FakeWeatherRepository(
            initial = Result.failure(Exception("network error")),
            subsequent = Result.success(weather)
        )
        val soilProfile = SoilProfile(
            lat = 14.9726,
            lon = -89.5301,
            dominantTexture = "Clay loam",
            domainHorizons = emptyList(),
        )
        val soilRepo = FakeSoilRepository(Result.success(soilProfile))
        val viewModel = HomeViewModel(geoRepo, weatherRepo, soilRepo)

        advanceUntilIdle()
        assertIs<HomeUiState.Error>(viewModel.uiState.value)

        weatherRepo.nextShouldSucceed = true
        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<HomeUiState.Data>(state)
        assertEquals("24.5°C", state.weather.temperatureCelsius)
    }

    @Test
    fun `init transitions Loading to Data with null soilSummary when soil fails`() = runTest(testDispatcher) {
        val location = ResolvedLocation(
            coordinates = LatLng(14.9726, -89.5301),
            display = LocationDisplay(
                primary = "Zacapa, Guatemala",
                municipality = "Zacapa",
                state = "Zacapa Department",
                country = "Guatemala",
            ),
            elevationMeters = null,
        )
        val weather = CurrentWeather(
            temperatureCelsius = "24.5°C",
            humidity = "78%",
            cloudCover = "65%",
            uvIndex = "3.0",
            description = "Día despejado",
            locationName = "",
            dateLabel = "2026-04-27",
        )
        val geoRepo = FakeGeolocationRepository(location)
        val weatherRepo = FakeWeatherRepository(Result.success(weather))
        val soilRepo = FakeSoilRepository(Result.failure(Exception("soil error")))
        val viewModel = HomeViewModel(geoRepo, weatherRepo, soilRepo)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<HomeUiState.Data>(state)
        assertEquals("24.5°C", state.weather.temperatureCelsius)
        assertNull(state.soilSummary)
    }

    private class FakeGeolocationRepository(
        private val resolved: ResolvedLocation?
    ) : GeolocationRepository {
        override suspend fun reverseGeocode(latLng: LatLng): Result<ResolvedLocation> =
            Result.failure(UnsupportedOperationException())

        override suspend fun saveResolvedLocation(location: ResolvedLocation) {}

        override fun observeResolvedLocation(): Flow<ResolvedLocation?> = flowOf(resolved)
    }

    private class FakeWeatherRepository(
        private val initial: Result<CurrentWeather>,
        private val subsequent: Result<CurrentWeather>? = null,
        var nextShouldSucceed: Boolean = false,
    ) : WeatherRepository {
        override suspend fun getCurrentWeather(latLng: LatLng): Result<CurrentWeather> {
            return if (nextShouldSucceed && subsequent != null) {
                subsequent
            } else {
                initial
            }
        }
    }

    private class FakeSoilRepository(
        private val result: Result<SoilProfile>,
    ) : SoilRepository {
        override suspend fun getSoil(latLng: LatLng): Result<SoilProfile> = result
    }
}
