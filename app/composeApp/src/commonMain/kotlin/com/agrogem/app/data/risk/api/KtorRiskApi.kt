package com.agrogem.app.data.risk.api

import com.agrogem.app.data.network.ApiError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class KtorRiskApi(private val client: HttpClient) : RiskApi {

    override suspend fun getDiseaseRisk(lat: Double, lon: Double, disease: String): DiseaseRiskResponse {
        return try {
            val response = client.get("/disease-risk") {
                parameter("lat", lat)
                parameter("lon", lon)
                parameter("disease", disease)
            }
            if (response.status.value in 200..299) {
                response.body<DiseaseRiskResponse>()
            } else {
                throw ApiError.from(response.status, response.body<String>())
            }
        } catch (e: ApiError) {
            throw e
        } catch (e: Exception) {
            throw ApiError.from(e)
        }
    }

    override suspend fun getPestRisk(lat: Double, lon: Double, pest: String): PestRiskResponse {
        return try {
            val response = client.get("/pest-risk") {
                parameter("lat", lat)
                parameter("lon", lon)
                parameter("pest", pest)
            }
            if (response.status.value in 200..299) {
                response.body<PestRiskResponse>()
            } else {
                throw ApiError.from(response.status, response.body<String>())
            }
        } catch (e: ApiError) {
            throw e
        } catch (e: Exception) {
            throw ApiError.from(e)
        }
    }
}
