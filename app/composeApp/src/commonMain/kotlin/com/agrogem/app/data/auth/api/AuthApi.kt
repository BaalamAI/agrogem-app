package com.agrogem.app.data.auth.api

import com.agrogem.app.data.network.ApiError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody

interface AuthApi {
    suspend fun register(phone: String, password: String): SessionResponse
    suspend fun login(phone: String, password: String): SessionResponse
}

class KtorAuthApi(private val client: HttpClient) : AuthApi {

    override suspend fun register(phone: String, password: String): SessionResponse {
        return postForSession("/users/register", RegisterRequest(phone = phone, password = password))
    }

    override suspend fun login(phone: String, password: String): SessionResponse {
        return postForSession("/users/login", LoginRequest(phone = phone, password = password))
    }

    private suspend fun postForSession(path: String, requestBody: Any): SessionResponse {
        return try {
            val response = client.post(path) {
                setBody(requestBody)
            }
            if (response.status.value in 200..299) {
                response.body<SessionResponse>()
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
