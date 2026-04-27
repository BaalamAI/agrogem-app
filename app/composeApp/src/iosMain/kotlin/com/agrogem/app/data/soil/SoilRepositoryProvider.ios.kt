package com.agrogem.app.data.soil

import com.agrogem.app.data.network.HttpClientFactory
import com.agrogem.app.data.soil.api.KtorSoilApi
import com.agrogem.app.data.soil.domain.SoilRepository
import com.agrogem.app.data.soil.domain.SoilRepositoryImpl
import io.ktor.client.engine.darwin.Darwin

actual fun createSoilRepository(): SoilRepository {
    val client = HttpClientFactory.create(engine = Darwin.create())
    val api = KtorSoilApi(client)
    return SoilRepositoryImpl(api = api)
}
