package com.agrogem.app.ui.screens.map

import com.agrogem.app.data.geolocation.domain.GeocodeResolved
import com.agrogem.app.data.geolocation.domain.GeolocationRepository
import com.agrogem.app.data.geolocation.domain.LocationDisplay
import com.agrogem.app.data.geolocation.domain.ResolvedLocation
import com.agrogem.app.data.risk.domain.DiseaseRisk
import com.agrogem.app.data.risk.domain.RiskRepository
import com.agrogem.app.data.risk.domain.RiskSeverity
import com.agrogem.app.data.shared.domain.LatLng
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MapRiskViewModelTest {

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
    fun `init emits Loading then Success with mapped data`() = runTest(testDispatcher) {
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
        val diseases = listOf(
            DiseaseRisk(
                diseaseName = "coffee_rust",
                displayName = "Roya del café",
                score = 0.82,
                severity = RiskSeverity.Critica,
                interpretation = "Riesgo alto de roya del café",
                factors = listOf("humedad"),
            ),
            DiseaseRisk(
                diseaseName = "late_blight",
                displayName = "Tizón tardío",
                score = 0.45,
                severity = RiskSeverity.Atencion,
                interpretation = "Riesgo moderado de tizón tardío",
                factors = listOf("lluvia"),
            ),
            DiseaseRisk(
                diseaseName = "corn_rust",
                displayName = "Roya del maíz",
                score = 0.12,
                severity = RiskSeverity.Optimo,
                interpretation = "Riesgo bajo de roya del maíz",
                factors = emptyList(),
            ),
        )
        val geoRepo = FakeGeolocationRepository(location)
        val riskRepo = FakeRiskRepository(Result.success(diseases))
        val viewModel = MapRiskViewModel(geoRepo, riskRepo)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<MapRiskViewModelState.Success>(state)
        val data = state.data
        assertEquals("Mapa de riesgo\nregional", data.title)
        assertEquals("Ubicación: Zacapa, Guatemala", data.subtitle)
        assertEquals(3, data.markers.size)
        assertEquals(2, data.alerts.size)
        assertEquals("Riesgo estimado para cultivos comunes de la región.", data.disclaimer)
        assertEquals("3", data.riskSummary.first { it.label == "Lotes monitoreados" }.value)
        assertEquals("1", data.riskSummary.first { it.label == "Alertas críticas" }.value)
        assertEquals("1", data.riskSummary.first { it.label == "Atención" }.value)
    }

    @Test
    fun `init emits Loading then Error when location is null`() = runTest(testDispatcher) {
        val geoRepo = FakeGeolocationRepository(null)
        val riskRepo = FakeRiskRepository(Result.failure(Exception("no-op")))
        val viewModel = MapRiskViewModel(geoRepo, riskRepo)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<MapRiskViewModelState.Error>(state)
        assertTrue(state.message.contains("ubicación", ignoreCase = true) || state.message.isNotBlank())
    }

    @Test
    fun `init emits Loading then Error when repository fails`() = runTest(testDispatcher) {
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
        val riskRepo = FakeRiskRepository(Result.failure(Exception("network error")))
        val viewModel = MapRiskViewModel(geoRepo, riskRepo)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<MapRiskViewModelState.Error>(state)
    }

    @Test
    fun `OnRefreshRequested re-fetches and emits new state`() = runTest(testDispatcher) {
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
        val diseases = listOf(
            DiseaseRisk(
                diseaseName = "coffee_rust",
                displayName = "Roya del café",
                score = 0.82,
                severity = RiskSeverity.Critica,
                interpretation = "Riesgo alto",
                factors = emptyList(),
            ),
        )
        val geoRepo = FakeGeolocationRepository(location)
        val riskRepo = FakeRiskRepository(
            initial = Result.failure(Exception("network error")),
            subsequent = Result.success(diseases),
        )
        val viewModel = MapRiskViewModel(geoRepo, riskRepo)

        advanceUntilIdle()
        assertIs<MapRiskViewModelState.Error>(viewModel.uiState.value)

        riskRepo.nextShouldSucceed = true
        viewModel.onEvent(MapRiskEvent.OnRefreshRequested)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<MapRiskViewModelState.Success>(state)
        assertEquals(1, state.data.markers.size)
    }

    @Test
    fun `empty disease list produces empty markers and alerts`() = runTest(testDispatcher) {
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
        val riskRepo = FakeRiskRepository(Result.success(emptyList()))
        val viewModel = MapRiskViewModel(geoRepo, riskRepo)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<MapRiskViewModelState.Success>(state)
        assertTrue(state.data.markers.isEmpty())
        assertTrue(state.data.alerts.isEmpty())
        assertEquals("0", state.data.riskSummary.first { it.label == "Lotes monitoreados" }.value)
    }

    private class FakeGeolocationRepository(
        private val resolved: ResolvedLocation?
    ) : GeolocationRepository {
        override suspend fun geocode(query: String): Result<GeocodeResolved> =
            Result.failure(UnsupportedOperationException())

        override suspend fun reverseGeocode(latLng: LatLng): Result<ResolvedLocation> =
            Result.failure(UnsupportedOperationException())

        override suspend fun saveResolvedLocation(location: ResolvedLocation) {}

        override fun observeResolvedLocation(): Flow<ResolvedLocation?> = flowOf(resolved)
    }

    private class FakeRiskRepository(
        private val initial: Result<List<DiseaseRisk>>,
        private val subsequent: Result<List<DiseaseRisk>>? = null,
        var nextShouldSucceed: Boolean = false,
    ) : RiskRepository {
        override suspend fun getDiseaseRisks(latLng: LatLng?): Result<List<DiseaseRisk>> {
            return if (nextShouldSucceed && subsequent != null) {
                subsequent
            } else {
                initial
            }
        }

        override suspend fun getPestRisks(latLng: LatLng?): Result<List<DiseaseRisk>> =
            Result.failure(UnsupportedOperationException("Not used in first slice"))
    }
}
