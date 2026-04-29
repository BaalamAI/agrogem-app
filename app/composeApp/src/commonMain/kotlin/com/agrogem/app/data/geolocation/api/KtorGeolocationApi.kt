package com.agrogem.app.data.geolocation.api

import com.agrogem.app.data.network.ApiError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class KtorGeolocationApi(private val client: HttpClient) : GeolocationApi {

    override suspend fun geocode(query: String): List<GeocodeHit> {
        return try {
            val response = client.get("/geocode") {
                parameter("q", query)
            }
            if (response.status.value in 200..299) {
                response.body<List<GeocodeHit>>()
            } else {
                throw ApiError.from(response.status, response.body<String>())
            }
        } catch (e: ApiError) {
            throw e
        } catch (e: Exception) {
            throw ApiError.from(e)
        }
    }

    override suspend fun reverseGeocode(lat: Double, lng: Double): ReverseGeocodeResponse {
        return try {
            val response = client.get("/geocode/reverse") {
                parameter("lat", lat)
                parameter("lon", lng)
            }
            if (response.status.value in 200..299) {
                response.body<ReverseGeocodeResponse>()
            } else {
                throw ApiError.from(response.status, response.body<String>())
            }
        } catch (e: ApiError) {
            throw e
        } catch (e: Exception) {
            throw ApiError.from(e)
        }
    }

    override suspend fun elevation(lat: Double, lng: Double): ElevationResponse {
        return try {
            val response = client.get("/elevation") {
                parameter("lat", lat)
                parameter("lon", lng)
            }
            if (response.status.value in 200..299) {
                response.body<ElevationResponse>()
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
