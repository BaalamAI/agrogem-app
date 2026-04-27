package com.agrogem.app.data.pest.api

import com.agrogem.app.data.network.ApiError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.client.plugins.timeout

class KtorPestApi(private val client: HttpClient) : PestApi {

    override suspend fun getUploadUrl(): PestUploadUrlResponse {
        return try {
            val response = client.post("/pest/upload-url")
            if (response.status.value in 200..299) {
                response.body<PestUploadUrlResponse>()
            } else {
                throw ApiError.from(response.status, response.body<String>())
            }
        } catch (e: ApiError) {
            throw e
        } catch (e: Exception) {
            throw ApiError.from(e)
        }
    }

    override suspend fun uploadImage(signedUrl: String, imageBytes: ByteArray) {
        try {
            val response = client.put {
                url(signedUrl)
                contentType(ContentType.Image.JPEG)
                setBody(imageBytes)
                timeout {
                    requestTimeoutMillis = 60_000
                }
            }
            if (response.status.value !in 200..299) {
                throw ApiError.from(response.status, response.body<String>())
            }
        } catch (e: ApiError) {
            throw e
        } catch (e: Exception) {
            throw ApiError.from(e)
        }
    }

    override suspend fun identify(objectPath: String): PestIdentifyResponse {
        return try {
            val response = client.post("/pest/identify") {
                setBody(mapOf("object_path" to objectPath))
                timeout {
                    requestTimeoutMillis = 45_000
                }
            }
            if (response.status.value in 200..299) {
                response.body<PestIdentifyResponse>()
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
