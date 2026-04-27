package com.agrogem.app.data.climate.api

import com.agrogem.app.data.network.ApiError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class KtorClimateApi(private val client: HttpClient) : ClimateApi {

    override suspend fun getClimateHistory(
        lat: Double,
        lon: Double,
        start: String,
        end: String,
        granularity: String,
    ): ClimateHistoryResponse {
        return try {
            val response = client.get("/climate/history") {
                parameter("lat", lat)
                parameter("lon", lon)
                parameter("start", start)
                parameter("end", end)
                parameter("granularity", granularity)
            }
            if (response.status.value in 200..299) {
                response.body<ClimateHistoryResponse>()
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
