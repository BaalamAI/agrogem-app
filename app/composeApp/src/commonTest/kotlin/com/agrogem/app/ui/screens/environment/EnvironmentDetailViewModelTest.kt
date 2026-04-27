package com.agrogem.app.ui.screens.environment

import com.agrogem.app.data.climate.domain.ClimateDataPoint
import com.agrogem.app.data.climate.domain.ClimateHistory
import com.agrogem.app.data.climate.domain.ClimateQuery
import com.agrogem.app.data.climate.domain.ClimateRepository
import com.agrogem.app.data.geolocation.domain.ResolvedLocation
import com.agrogem.app.data.shared.domain.LatLng
import com.agrogem.app.data.soil.domain.Horizon
import com.agrogem.app.data.soil.domain.SoilProfile
import com.agrogem.app.data.soil.domain.SoilRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EnvironmentDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val location = ResolvedLocation(
        coordinates = LatLng(14.9726, -89.5301),
        display = com.agrogem.app.data.geolocation.domain.LocationDisplay(
            primary = "Test Location",
            municipality = null,
            state = null,
            country = null,
        ),
        elevationMeters = 1200.0,
    )

    private val soilProfile = SoilProfile(
        lat = 14.9726,
        lon = -89.5301,
        dominantTexture = "Clay loam",
        domainHorizons = listOf(
            Horizon(depth = "0-5cm", ph = 6.2, textureClass = "Clay loam", socGPerKg = 12.5)
        ),
        interpretation = "Suelo franco arcilloso",
    )

    private val climateHistory = ClimateHistory(
        lat = 14.9726,
        lon = -89.5301,
        granularity = "monthly",
        domainSeries = listOf(
            ClimateDataPoint(
                date = "2024-01",
                t2m = 22.5,
                t2mMax = 28.0,
                t2mMin = 17.0,
                precipitationMm = 45.2,
                rhPct = 78.0,
                solarMjM2 = 15.3,
            )
        ),
    )

    private val query = ClimateQuery(start = "2024-01-01", end = "2024-12-31", granularity = "monthly")

    @Test
    fun `initial state is Loading`() = runTest {
        val vm = EnvironmentDetailViewModel(
            soilRepository = FakeSoilRepository(Result.success(soilProfile)),
            climateRepository = FakeClimateRepository(Result.success(climateHistory)),
            location = location,
            query = query,
        )

        assertIs<EnvironmentDetailUiState.Loading>(vm.uiState.value)
    }

    @Test
    fun `parallel load success emits Success with soil and climate`() = runTest {
        val vm = EnvironmentDetailViewModel(
            soilRepository = FakeSoilRepository(Result.success(soilProfile)),
            climateRepository = FakeClimateRepository(Result.success(climateHistory)),
            location = location,
            query = query,
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertIs<EnvironmentDetailUiState.Success>(state)
        assertEquals(soilProfile, state.soil)
        assertEquals(climateHistory, state.climate)
        assertEquals("Suelo franco arcilloso", state.interpretation)
    }

    @Test
    fun `soil fails climate succeeds emits Success with partial data`() = runTest {
        val vm = EnvironmentDetailViewModel(
            soilRepository = FakeSoilRepository(Result.failure(RuntimeException("Soil error"))),
            climateRepository = FakeClimateRepository(Result.success(climateHistory)),
            location = location,
            query = query,
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertIs<EnvironmentDetailUiState.Success>(state)
        assertNull(state.soil)
        assertEquals(climateHistory, state.climate)
    }

    @Test
    fun `climate fails soil succeeds emits Success with partial data`() = runTest {
        val vm = EnvironmentDetailViewModel(
            soilRepository = FakeSoilRepository(Result.success(soilProfile)),
            climateRepository = FakeClimateRepository(Result.failure(RuntimeException("Climate error"))),
            location = location,
            query = query,
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertIs<EnvironmentDetailUiState.Success>(state)
        assertEquals(soilProfile, state.soil)
        assertNull(state.climate)
    }

    @Test
    fun `both fail emits Error with aggregate message`() = runTest {
        val vm = EnvironmentDetailViewModel(
            soilRepository = FakeSoilRepository(Result.failure(RuntimeException("Soil error"))),
            climateRepository = FakeClimateRepository(Result.failure(RuntimeException("Climate error"))),
            location = location,
            query = query,
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertIs<EnvironmentDetailUiState.Error>(state)
        assertTrue(state.canRetry)
        assertTrue(state.message.contains("Soil error") || state.message.contains("Climate error"))
    }

    @Test
    fun `success state includes location name and elevation`() = runTest {
        val vm = EnvironmentDetailViewModel(
            soilRepository = FakeSoilRepository(Result.success(soilProfile)),
            climateRepository = FakeClimateRepository(Result.success(climateHistory)),
            location = location,
            query = query,
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertIs<EnvironmentDetailUiState.Success>(state)
        assertEquals("Test Location", state.locationName)
        assertEquals(1200.0, state.elevationMeters)
    }

    @Test
    fun `success state handles null elevation`() = runTest {
        val locationWithNullElevation = ResolvedLocation(
            coordinates = LatLng(14.9726, -89.5301),
            display = com.agrogem.app.data.geolocation.domain.LocationDisplay(
                primary = "Null Elevation Location",
                municipality = null,
                state = null,
                country = null,
            ),
            elevationMeters = null,
        )
        val vm = EnvironmentDetailViewModel(
            soilRepository = FakeSoilRepository(Result.success(soilProfile)),
            climateRepository = FakeClimateRepository(Result.success(climateHistory)),
            location = locationWithNullElevation,
            query = query,
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertIs<EnvironmentDetailUiState.Success>(state)
        assertEquals("Null Elevation Location", state.locationName)
        assertEquals(null, state.elevationMeters)
    }

    @Test
    fun `retry reloads data and transitions from Error to Success`() = runTest {
        var soilResult: Result<SoilProfile> = Result.failure(RuntimeException("Soil error"))
        var climateResult: Result<ClimateHistory> = Result.failure(RuntimeException("Climate error"))
        val soilRepo = FakeSoilRepository { soilResult }
        val climateRepo = FakeClimateRepository { climateResult }
        val vm = EnvironmentDetailViewModel(
            soilRepository = soilRepo,
            climateRepository = climateRepo,
            location = location,
            query = query,
        )

        testDispatcher.scheduler.advanceUntilIdle()
        assertIs<EnvironmentDetailUiState.Error>(vm.uiState.value)

        soilResult = Result.success(soilProfile)
        climateResult = Result.success(climateHistory)
        vm.retry()
        testDispatcher.scheduler.advanceUntilIdle()

        val successState = vm.uiState.value
        assertIs<EnvironmentDetailUiState.Success>(successState)
        assertNotNull(successState.soil)
        assertNotNull(successState.climate)
    }

    private class FakeSoilRepository(
        private val resultProvider: () -> Result<SoilProfile>,
    ) : SoilRepository {
        constructor(result: Result<SoilProfile>) : this({ result })

        override suspend fun getSoil(latLng: LatLng): Result<SoilProfile> = resultProvider()
    }

    private class FakeClimateRepository(
        private val resultProvider: () -> Result<ClimateHistory>,
    ) : ClimateRepository {
        constructor(result: Result<ClimateHistory>) : this({ result })

        override suspend fun getClimateHistory(
            latLng: LatLng,
            start: String,
            end: String,
            granularity: String,
        ): Result<ClimateHistory> = resultProvider()
    }
}
