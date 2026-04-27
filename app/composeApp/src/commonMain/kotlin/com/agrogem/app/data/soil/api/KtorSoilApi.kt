package com.agrogem.app.data.soil.api

import com.agrogem.app.data.network.ApiError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class KtorSoilApi(private val client: HttpClient) : SoilApi {

    override suspend fun getSoil(lat: Double, lon: Double): SoilResponse {
        return try {
            val response = client.get("/soil") {
                parameter("lat", lat)
                parameter("lon", lon)
            }
            if (response.status.value in 200..299) {
                response.body<SoilResponse>()
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
