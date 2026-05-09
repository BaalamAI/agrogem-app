package com.agrogem.app.data.risk.api

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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RiskApiTest {

    @Test
    fun `getDiseaseRisk returns full response with lat lon disease params`() = runTest {
        val mockEngine = MockEngine { request ->
            assertTrue(request.url.encodedPath.endsWith("/disease-risk"))
            assertEquals("14.9726", request.url.parameters["lat"])
            assertEquals("-89.5301", request.url.parameters["lon"])
            assertEquals("coffee_rust", request.url.parameters["disease"])
            respond(
                content = """{
                    "disease": "coffee_rust",
                    "risk_score": 0.82,
                    "risk_level": "high",
                    "factors": ["humedad", "temperatura"],
                    "interpretation": "Riesgo alto de roya del café"
                }""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val api: RiskApi = KtorRiskApi(HttpClientFactory.create(engine = mockEngine))

        val result = api.getDiseaseRisk(lat = 14.9726, lon = -89.5301, disease = "coffee_rust")

        assertEquals("coffee_rust", result.disease)
        assertEquals(0.82, result.riskScore)
        assertEquals("high", result.riskLevel)
        assertEquals(listOf("humedad", "temperatura"), result.factors?.jsonArray?.map { it.jsonPrimitive.content })
        assertEquals("Riesgo alto de roya del café", result.interpretation)
    }

    @Test
    fun `getDiseaseRisk decodes empty JSON with null defaults safely`() = runTest {
        // Defensive deserialization: missing fields default to null rather than crashing.
        // Repository mapping applies safe fallbacks (?:) downstream.
        val mockEngine = MockEngine {
            respond(
                content = "{}",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val api: RiskApi = KtorRiskApi(HttpClientFactory.create(engine = mockEngine))

        val result = api.getDiseaseRisk(lat = 0.0, lon = 0.0, disease = "coffee_rust")

        assertNull(result.disease)
        assertNull(result.riskScore)
        assertNull(result.riskLevel)
        assertNull(result.factors)
        assertNull(result.interpretation)
    }

    @Test
    fun `getDiseaseRisk returns ServerError on 500`() = runTest {
        val mockEngine = MockEngine {
            respondError(status = HttpStatusCode.InternalServerError)
        }

        val api: RiskApi = KtorRiskApi(HttpClientFactory.create(engine = mockEngine))

        val error = runCatching { api.getDiseaseRisk(0.0, 0.0, "coffee_rust") }.exceptionOrNull()

        assertIs<ApiError.ServerError>(error)
    }

    @Test
    fun `getDiseaseRisk returns NetworkError on engine failure`() = runTest {
        val mockEngine = MockEngine {
            throw RuntimeException("No connection")
        }

        val api: RiskApi = KtorRiskApi(HttpClientFactory.create(engine = mockEngine))

        val error = runCatching { api.getDiseaseRisk(0.0, 0.0, "coffee_rust") }.exceptionOrNull()

        assertIs<ApiError.NetworkError>(error)
    }

    @Test
    fun `getPestRisk returns full response with lat lon pest params`() = runTest {
        val mockEngine = MockEngine { request ->
            assertTrue(request.url.encodedPath.endsWith("/pest-risk"))
            assertEquals("14.9726", request.url.parameters["lat"])
            assertEquals("-89.5301", request.url.parameters["lon"])
            assertEquals("spider_mite", request.url.parameters["pest"])
            respond(
                content = """{
                    "pest": "spider_mite",
                    "pest_type": "ácaro",
                    "life_stage_risk": { "egg": 0.1, "larva": 0.5, "adult": 0.9 },
                    "affected_crops": ["frijol", "maíz"],
                    "risk_score": 0.75,
                    "risk_level": "moderate",
                    "virus_coalert": null,
                    "factors": ["sequía"],
                    "interpretation": "Riesgo moderado de araña roja"
                }""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val api: RiskApi = KtorRiskApi(HttpClientFactory.create(engine = mockEngine))

        val result = api.getPestRisk(lat = 14.9726, lon = -89.5301, pest = "spider_mite")

        assertEquals("spider_mite", result.pest)
        assertEquals("ácaro", result.pestType)
        assertEquals(listOf("frijol", "maíz"), result.affectedCrops)
        assertEquals(0.75, result.riskScore)
        assertEquals("moderate", result.riskLevel)
        assertNull(result.virusCoalert)
        assertEquals(listOf("sequía"), result.factors?.jsonArray?.map { it.jsonPrimitive.content })
    }

    @Test
    fun `getPestRisk returns ServerError on 500`() = runTest {
        val mockEngine = MockEngine {
            respondError(status = HttpStatusCode.InternalServerError)
        }

        val api: RiskApi = KtorRiskApi(HttpClientFactory.create(engine = mockEngine))

        val error = runCatching { api.getPestRisk(0.0, 0.0, "spider_mite") }.exceptionOrNull()

        assertIs<ApiError.ServerError>(error)
    }
}
