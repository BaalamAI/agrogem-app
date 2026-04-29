package com.agrogem.app.ui.screens.home

import com.agrogem.app.data.geolocation.domain.GeolocationRepository
import com.agrogem.app.data.geolocation.domain.LocationDisplay
import com.agrogem.app.data.geolocation.domain.ResolvedLocation
import com.agrogem.app.data.location.DeviceLocationProvider
import com.agrogem.app.data.shared.domain.LatLng
import com.agrogem.app.data.soil.domain.SoilProfile
import com.agrogem.app.data.soil.domain.SoilRepository
import com.agrogem.app.data.session.SessionLocalStore
import com.agrogem.app.data.session.SessionSnapshot
import com.agrogem.app.data.weather.domain.CurrentWeather
import com.agrogem.app.data.weather.domain.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
            precipitation = "0.8 mm",
            weatherCode = 1,
            windSpeed = "12 km/h",
            maxMin = "30°/19°",
            uvIndex = "3.0",
            description = "Día despejado",
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
        val viewModel = HomeViewModel(geoRepo, weatherRepo, soilRepo, SessionLocalStore())

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<HomeUiState.Data>(state)
        assertEquals("Zacapa, Guatemala", state.locationInfo.display.primary)
        assertEquals("24.5°C", state.weather.temperatureCelsius)
        assertEquals("78%", state.metrics.humidity)
        assertEquals("12 km/h", state.metrics.windSpeed)
        assertEquals("Clay loam", state.soilSummary?.dominantTexture)
        assertNull(state.soilSummary?.topHorizonPh)
        assertNull(state.profileGreeting)
        assertNull(state.cropContext)
    }

    @Test
    fun `init transitions Loading to LocationMissing when store empty`() = runTest(testDispatcher) {
        val geoRepo = FakeGeolocationRepository(null)
        val weatherRepo = FakeWeatherRepository(Result.failure(Exception("no-op")))
        val soilRepo = FakeSoilRepository(Result.failure(Exception("no-op")))
        val viewModel = HomeViewModel(geoRepo, weatherRepo, soilRepo, SessionLocalStore())

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<HomeUiState.LocationMissing>(state)
    }

    @Test
    fun `init transitions from LocationMissing to Data when location appears later`() = runTest(testDispatcher) {
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
            precipitation = "0.8 mm",
            weatherCode = 1,
            windSpeed = "12 km/h",
            maxMin = "30°/19°",
            uvIndex = "3.0",
            description = "Día despejado",
            dateLabel = "2026-04-27",
        )
        val geoRepo = FakeGeolocationRepository(null)
        val weatherRepo = FakeWeatherRepository(Result.success(weather))
        val soilRepo = FakeSoilRepository(Result.failure(Exception("soil error")))
        val viewModel = HomeViewModel(geoRepo, weatherRepo, soilRepo, SessionLocalStore())

        advanceUntilIdle()
        assertIs<HomeUiState.LocationMissing>(viewModel.uiState.value)

        geoRepo.emit(location)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<HomeUiState.Data>(state)
        assertEquals("24.5°C", state.weather.temperatureCelsius)
        assertEquals("Zacapa, Guatemala", state.locationInfo.display.primary)
    }

    @Test
    fun `init auto loads data when current location provider succeeds`() = runTest(testDispatcher) {
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
            precipitation = "0.8 mm",
            weatherCode = 1,
            windSpeed = "12 km/h",
            maxMin = "30°/19°",
            uvIndex = "3.0",
            description = "Día despejado",
            dateLabel = "2026-04-27",
        )
        val geoRepo = FakeGeolocationRepository(null, reverseGeocodeResult = Result.success(location))
        val weatherRepo = FakeWeatherRepository(Result.success(weather))
        val soilRepo = FakeSoilRepository(Result.failure(Exception("soil error")))
        val viewModel = HomeViewModel(
            geoRepo,
            weatherRepo,
            soilRepo,
            SessionLocalStore(),
            FakeDeviceLocationProvider(Result.success(location.coordinates)),
        )

        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertIs<HomeUiState.Data>(state)
        assertEquals("Zacapa, Guatemala", state.locationInfo.display.primary)
        assertEquals(1, geoRepo.reverseGeocodeCalls)
    }

    @Test
    fun `location permission result shows error when current location fails`() = runTest(testDispatcher) {
        val geoRepo = FakeGeolocationRepository(null)
        val weatherRepo = FakeWeatherRepository(Result.failure(Exception("no-op")))
        val soilRepo = FakeSoilRepository(Result.failure(Exception("no-op")))
        val viewModel = HomeViewModel(
            geoRepo,
            weatherRepo,
            soilRepo,
            SessionLocalStore(),
            FakeDeviceLocationProvider(Result.failure(Exception("gps apagado"))),
        )

        advanceUntilIdle()
        viewModel.onLocationPermissionResult(granted = true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<HomeUiState.Error>(state)
        assertEquals("gps apagado", state.message)
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
        val viewModel = HomeViewModel(geoRepo, weatherRepo, soilRepo, SessionLocalStore())

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
            precipitation = "0.8 mm",
            weatherCode = 1,
            windSpeed = "12 km/h",
            maxMin = "30°/19°",
            uvIndex = "3.0",
            description = "Día despejado",
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
        val viewModel = HomeViewModel(geoRepo, weatherRepo, soilRepo, SessionLocalStore())

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
            precipitation = "0.8 mm",
            weatherCode = 1,
            windSpeed = "12 km/h",
            maxMin = "30°/19°",
            uvIndex = "3.0",
            description = "Día despejado",
            dateLabel = "2026-04-27",
        )
        val geoRepo = FakeGeolocationRepository(location)
        val weatherRepo = FakeWeatherRepository(Result.success(weather))
        val soilRepo = FakeSoilRepository(Result.failure(Exception("soil error")))
        val viewModel = HomeViewModel(geoRepo, weatherRepo, soilRepo, SessionLocalStore())

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<HomeUiState.Data>(state)
        assertEquals("24.5°C", state.weather.temperatureCelsius)
        assertNull(state.soilSummary)
    }

    @Test
    fun `init includes onboarding profile greeting when crops exist`() = runTest(testDispatcher) {
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
            precipitation = "0.8 mm",
            weatherCode = 1,
            windSpeed = "12 km/h",
            maxMin = "30°/19°",
            uvIndex = "3.0",
            description = "Día despejado",
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
        val sessionStore = SessionLocalStore()
        sessionStore.write(
            SessionSnapshot(
                onboardingDone = true,
                name = "Kevin",
                crops = "maíz y frijol",
                area = "Parcela Norte",
                stage = "floración",
            )
        )
        val viewModel = HomeViewModel(geoRepo, weatherRepo, soilRepo, sessionStore)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<HomeUiState.Data>(state)
        assertEquals("Hola, Kevin. Hoy te acompaño con maíz y frijol en etapa floración en Parcela Norte.", state.profileGreeting)
        assertEquals("maíz y frijol · floración", state.cropContext)
    }

    private class FakeGeolocationRepository(
        resolved: ResolvedLocation?,
        private val reverseGeocodeResult: Result<ResolvedLocation> = Result.failure(UnsupportedOperationException()),
    ) : GeolocationRepository {
        private val state = MutableStateFlow(resolved)
        var reverseGeocodeCalls: Int = 0

        override suspend fun reverseGeocode(latLng: LatLng): Result<ResolvedLocation> {
            reverseGeocodeCalls += 1
            reverseGeocodeResult.onSuccess { state.value = it }
            return reverseGeocodeResult
        }

        override suspend fun saveResolvedLocation(location: ResolvedLocation) {}

        fun emit(location: ResolvedLocation?) {
            state.value = location
        }

        override fun observeResolvedLocation(): Flow<ResolvedLocation?> = state
    }

    private class FakeDeviceLocationProvider(
        private val result: Result<LatLng>,
    ) : DeviceLocationProvider {
        override suspend fun getCurrentLatLng(): Result<LatLng> = result
    }

    private class FakeWeatherRepository(
        private val initial: Result<CurrentWeather>,
        private val subsequent: Result<CurrentWeather>? = null,
        var nextShouldSucceed: Boolean = false,
        private val cached: CurrentWeather? = null,
        private val refreshNeeded: Boolean = true,
    ) : WeatherRepository {
        override suspend fun getCurrentWeather(latLng: LatLng): Result<CurrentWeather> {
            return if (nextShouldSucceed && subsequent != null) {
                subsequent
            } else {
                initial
            }
        }

        override suspend fun getCachedWeather(latLng: LatLng): CurrentWeather? = cached

        override suspend fun shouldRefresh(latLng: LatLng): Boolean = refreshNeeded
    }

    private class FakeSoilRepository(
        private val result: Result<SoilProfile>,
    ) : SoilRepository {
        override suspend fun getSoil(latLng: LatLng): Result<SoilProfile> = result
    }
}
