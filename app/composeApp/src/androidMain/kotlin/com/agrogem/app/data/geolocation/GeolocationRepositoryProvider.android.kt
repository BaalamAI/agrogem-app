package com.agrogem.app.data.geolocation

import com.agrogem.app.data.geolocation.api.KtorGeolocationApi
import com.agrogem.app.data.geolocation.domain.GeolocationRepository
import com.agrogem.app.data.geolocation.domain.GeolocationRepositoryImpl
import com.agrogem.app.data.network.HttpClientFactory
import io.ktor.client.engine.okhttp.OkHttp

actual fun createGeolocationRepository(): GeolocationRepository {
    val client = HttpClientFactory.create(engine = OkHttp.create())
    val api = KtorGeolocationApi(client)
    val store = ResolvedLocationStore()
    return GeolocationRepositoryImpl(api = api, store = store)
}
