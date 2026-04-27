package com.agrogem.app.data.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object HttpClientFactory {

    fun create(
        engine: HttpClientEngine,
        loggingEnabled: Boolean = false,
        socketTimeoutMs: Long = 15_000,
    ): HttpClient = HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }

        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = socketTimeoutMs
            requestTimeoutMillis = 30_000
        }

        if (loggingEnabled) {
            install(Logging) {
                level = LogLevel.HEADERS
            }
        }

        defaultRequest {
            url(BackendConfig.BASE_URL)
            contentType(ContentType.Application.Json)
        }

        expectSuccess = false
    }
}
