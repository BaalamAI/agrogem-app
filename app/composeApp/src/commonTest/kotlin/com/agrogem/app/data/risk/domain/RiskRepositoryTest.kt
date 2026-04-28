package com.agrogem.app.data.risk.domain

import com.agrogem.app.data.network.ApiError
import com.agrogem.app.data.risk.api.DiseaseRiskResponse
import com.agrogem.app.data.risk.api.PestRiskResponse
import com.agrogem.app.data.risk.api.RiskApi
import com.agrogem.app.data.shared.domain.LatLng
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RiskRepositoryTest {

    @Test
    fun `getDiseaseRisks maps all fields on happy path`() = runTest {
        val fakeApi = FakeRiskApi(
            diseaseResponses = mapOf(
                "coffee_rust" to DiseaseRiskResponse(
                    disease = "coffee_rust",
                    riskScore = 0.82,
                    riskLevel = "high",
                    factors = listOf("humedad", "temperatura"),
                    interpretation = "Riesgo alto de roya del café",
                ),
                "late_blight" to DiseaseRiskResponse(
                    disease = "late_blight",
                    riskScore = 0.45,
                    riskLevel = "moderate",
                    factors = listOf("lluvia"),
                    interpretation = "Riesgo moderado de tizón tardío",
                ),
                "corn_rust" to DiseaseRiskResponse(
                    disease = "corn_rust",
                    riskScore = 0.12,
                    riskLevel = "low",
                    factors = emptyList(),
                    interpretation = "Riesgo bajo de roya del maíz",
                ),
            )
        )
        val repo = RiskRepositoryImpl(api = fakeApi)

        val result = repo.getDiseaseRisks(LatLng(14.9726, -89.5301))

        assertTrue(result.isSuccess)
        val risks = result.getOrNull()!!
        assertEquals(3, risks.size)

        val coffee = risks[0]
        assertEquals("coffee_rust", coffee.diseaseName)
        assertEquals("Roya del café", coffee.displayName)
        assertEquals(0.82, coffee.score)
        assertEquals(RiskSeverity.Critica, coffee.severity)
        assertEquals("Riesgo alto de roya del café", coffee.interpretation)
        assertEquals(listOf("humedad", "temperatura"), coffee.factors)

        val late = risks[1]
        assertEquals("late_blight", late.diseaseName)
        assertEquals("Tizón tardío", late.displayName)
        assertEquals(0.45, late.score)
        assertEquals(RiskSeverity.Atencion, late.severity)

        val corn = risks[2]
        assertEquals("corn_rust", corn.diseaseName)
        assertEquals("Roya del maíz", corn.displayName)
        assertEquals(0.12, corn.score)
        assertEquals(RiskSeverity.Optimo, corn.severity)
    }

    @Test
    fun `getDiseaseRisks returns failure when latLng is null`() = runTest {
        val fakeApi = FakeRiskApi()
        val repo = RiskRepositoryImpl(api = fakeApi)

        val result = repo.getDiseaseRisks(null)

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()!!
        assertIs<IllegalStateException>(error)
        assertEquals("Sin ubicación", error.message)
    }

    @Test
    fun `getDiseaseRisks returns failure on API error`() = runTest {
        val fakeApi = FakeRiskApi(diseaseError = ApiError.ServerError)
        val repo = RiskRepositoryImpl(api = fakeApi)

        val result = repo.getDiseaseRisks(LatLng(0.0, 0.0))

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()!!
        assertIs<ApiError.ServerError>(error)
    }

    @Test
    fun `getDiseaseRisks maps unknown severity to Atencion`() = runTest {
        val fakeApi = FakeRiskApi(
            diseaseResponses = mapOf(
                "coffee_rust" to DiseaseRiskResponse(
                    disease = "coffee_rust",
                    riskScore = 0.5,
                    riskLevel = "unknown",
                    factors = emptyList(),
                    interpretation = "Nivel desconocido",
                ),
            )
        )
        val repo = RiskRepositoryImpl(api = fakeApi)

        val result = repo.getDiseaseRisks(LatLng(0.0, 0.0))

        assertTrue(result.isSuccess)
        val risks = result.getOrNull()!!
        assertEquals(RiskSeverity.Atencion, risks[0].severity)
    }

    @Test
    fun `getDiseaseRisks maps very_high severity to Critica`() = runTest {
        val fakeApi = FakeRiskApi(
            diseaseResponses = mapOf(
                "coffee_rust" to DiseaseRiskResponse(
                    disease = "coffee_rust",
                    riskScore = 0.95,
                    riskLevel = "very_high",
                    factors = listOf("humedad extrema"),
                    interpretation = "Riesgo crítico de roya del café",
                ),
            )
        )
        val repo = RiskRepositoryImpl(api = fakeApi)

        val result = repo.getDiseaseRisks(LatLng(0.0, 0.0))

        assertTrue(result.isSuccess)
        val risks = result.getOrNull()!!
        assertEquals(RiskSeverity.Critica, risks[0].severity)
        assertEquals(0.95, risks[0].score)
        assertEquals("Riesgo crítico de roya del café", risks[0].interpretation)
    }

    @Test
    fun `getDiseaseRisks handles null DTO fields with safe defaults`() = runTest {
        val fakeApi = FakeRiskApi(
            diseaseResponses = mapOf(
                "coffee_rust" to DiseaseRiskResponse(),
            )
        )
        val repo = RiskRepositoryImpl(api = fakeApi)

        val result = repo.getDiseaseRisks(LatLng(0.0, 0.0))

        assertTrue(result.isSuccess)
        val risks = result.getOrNull()!!
        assertEquals(3, risks.size)

        val coffee = risks[0]
        assertEquals("coffee_rust", coffee.diseaseName)
        assertEquals("Roya del café", coffee.displayName)
        assertEquals(0.0, coffee.score)
        assertEquals(RiskSeverity.Atencion, coffee.severity)
        assertEquals("", coffee.interpretation)
        assertEquals(emptyList(), coffee.factors)
    }

    @Test
    fun `mapDiseaseBackend converts snake case to display name`() {
        assertEquals("Roya del café", mapDiseaseBackend("coffee_rust"))
        assertEquals("Tizón tardío", mapDiseaseBackend("late_blight"))
        assertEquals("Roya del maíz", mapDiseaseBackend("corn_rust"))
        assertEquals("Ácaro arañero", mapDiseaseBackend("spider_mite"))
        assertEquals("Mosca blanca", mapDiseaseBackend("whitefly"))
        assertEquals("Broca del café", mapDiseaseBackend("coffee_berry_borer"))
        assertEquals("Otra enfermedad", mapDiseaseBackend("otra_enfermedad"))
    }

    @Test
    fun `getPestRisks maps all fields on happy path`() = runTest {
        val fakeApi = FakeRiskApi(
            pestResponses = mapOf(
                "spider_mite" to PestRiskResponse(
                    pest = "spider_mite",
                    riskScore = 0.75,
                    riskLevel = "moderate",
                    factors = listOf("baja humedad"),
                    interpretation = "Riesgo moderado de ácaro arañero",
                ),
                "whitefly" to PestRiskResponse(
                    pest = "whitefly",
                    riskScore = 0.88,
                    riskLevel = "high",
                    factors = listOf("temperatura alta"),
                    interpretation = "Riesgo alto de mosca blanca",
                ),
            )
        )
        val repo = RiskRepositoryImpl(api = fakeApi)

        val result = repo.getPestRisks(LatLng(14.9726, -89.5301))

        assertTrue(result.isSuccess)
        val risks = result.getOrNull()!!
        assertTrue(risks.size >= 2)

        val spider = risks.first { it.diseaseName == "spider_mite" }
        assertEquals("Ácaro arañero", spider.displayName)
        assertEquals(0.75, spider.score)
        assertEquals(RiskSeverity.Atencion, spider.severity)
        assertEquals("Riesgo moderado de ácaro arañero", spider.interpretation)

        val whitefly = risks.first { it.diseaseName == "whitefly" }
        assertEquals("Mosca blanca", whitefly.displayName)
        assertEquals(0.88, whitefly.score)
        assertEquals(RiskSeverity.Critica, whitefly.severity)
    }

    @Test
    fun `getPestRisks returns failure when latLng is null`() = runTest {
        val fakeApi = FakeRiskApi()
        val repo = RiskRepositoryImpl(api = fakeApi)

        val result = repo.getPestRisks(null)

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()!!
        assertIs<IllegalStateException>(error)
        assertEquals("Sin ubicación", error.message)
    }

    @Test
    fun `getPestRisks returns failure on API error`() = runTest {
        val fakeApi = FakeRiskApi(pestError = ApiError.ServerError)
        val repo = RiskRepositoryImpl(api = fakeApi)

        val result = repo.getPestRisks(LatLng(0.0, 0.0))

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()!!
        assertIs<ApiError.ServerError>(error)
    }

    private class FakeRiskApi(
        private val diseaseResponses: Map<String, DiseaseRiskResponse> = emptyMap(),
        private val pestResponses: Map<String, PestRiskResponse> = emptyMap(),
        private val diseaseError: ApiError? = null,
        private val pestError: ApiError? = null,
    ) : RiskApi {
        override suspend fun getDiseaseRisk(lat: Double, lon: Double, disease: String): DiseaseRiskResponse {
            if (diseaseError != null) throw diseaseError
            return diseaseResponses[disease] ?: DiseaseRiskResponse()
        }

        override suspend fun getPestRisk(lat: Double, lon: Double, pest: String): PestRiskResponse {
            if (pestError != null) throw pestError
            return pestResponses[pest] ?: PestRiskResponse()
        }
    }
}
