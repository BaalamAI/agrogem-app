package com.agrogem.app.data.weather.api

import com.agrogem.app.data.network.ApiError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class KtorWeatherApi(private val client: HttpClient) : WeatherApi {

    override suspend fun getCurrentWeather(lat: Double, lon: Double): WeatherResponse {
        return try {
            val response = client.get("/weather") {
                parameter("lat", lat)
                parameter("lon", lon)
            }
            if (response.status.value in 200..299) {
                response.body<WeatherResponse>()
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
