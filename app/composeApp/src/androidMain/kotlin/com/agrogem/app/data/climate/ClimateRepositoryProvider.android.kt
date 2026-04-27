package com.agrogem.app.data.climate

import com.agrogem.app.data.climate.api.KtorClimateApi
import com.agrogem.app.data.climate.domain.ClimateRepository
import com.agrogem.app.data.climate.domain.ClimateRepositoryImpl
import com.agrogem.app.data.network.HttpClientFactory
import io.ktor.client.engine.okhttp.OkHttp

actual fun createClimateRepository(): ClimateRepository {
    val client = HttpClientFactory.create(engine = OkHttp.create(), socketTimeoutMs = 20_000)
    val api = KtorClimateApi(client)
    return ClimateRepositoryImpl(api = api)
}
