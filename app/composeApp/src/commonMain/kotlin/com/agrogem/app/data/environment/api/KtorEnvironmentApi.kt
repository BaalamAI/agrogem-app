package com.agrogem.app.data.environment.api

import com.agrogem.app.data.network.ApiError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class KtorEnvironmentApi(private val client: HttpClient) : EnvironmentApi {

    override suspend fun getEnvironment(lat: Double, lon: Double): EnvironmentResponseDto {
        return try {
            val response = client.get("/environment") {
                parameter("lat", lat)
                parameter("lon", lon)
            }
            if (response.status.value in 200..299) {
                response.body()
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
