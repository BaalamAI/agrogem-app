package com.agrogem.app.data.pest

import com.agrogem.app.data.network.HttpClientFactory
import com.agrogem.app.data.pest.api.KtorPestApi
import com.agrogem.app.data.pest.domain.PestRepository
import com.agrogem.app.data.pest.domain.PestRepositoryImpl
import io.ktor.client.engine.okhttp.OkHttp

actual fun createPestRepository(): PestRepository {
    val client = HttpClientFactory.create(engine = OkHttp.create())
    val api = KtorPestApi(client)
    return PestRepositoryImpl(api = api)
}
