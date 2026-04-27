package com.agrogem.app.data.risk

import com.agrogem.app.data.network.HttpClientFactory
import com.agrogem.app.data.risk.api.KtorRiskApi
import com.agrogem.app.data.risk.domain.RiskRepository
import com.agrogem.app.data.risk.domain.RiskRepositoryImpl
import io.ktor.client.engine.darwin.Darwin

actual fun createRiskRepository(): RiskRepository {
    val client = HttpClientFactory.create(engine = Darwin.create())
    val api = KtorRiskApi(client)
    return RiskRepositoryImpl(api = api)
}
