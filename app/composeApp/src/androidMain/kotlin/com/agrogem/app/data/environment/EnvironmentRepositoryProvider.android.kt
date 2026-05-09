package com.agrogem.app.data.environment

import com.agrogem.app.data.environment.api.KtorEnvironmentApi
import com.agrogem.app.data.environment.domain.EnvironmentRepository
import com.agrogem.app.data.environment.domain.EnvironmentRepositoryImpl
import com.agrogem.app.data.network.HttpClientFactory
import io.ktor.client.engine.okhttp.OkHttp

actual fun createEnvironmentRepository(): EnvironmentRepository {
    val client = HttpClientFactory.create(engine = OkHttp.create())
    return EnvironmentRepositoryImpl(api = KtorEnvironmentApi(client))
}
