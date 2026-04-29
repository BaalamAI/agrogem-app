package com.agrogem.app.data.weather

import com.agrogem.app.data.network.HttpClientFactory
import com.agrogem.app.data.weather.api.KtorWeatherApi
import com.agrogem.app.data.weather.domain.WeatherRepository
import com.agrogem.app.data.weather.domain.WeatherRepositoryImpl
import com.agrogem.app.data.weather.domain.createWeatherCacheStore
import io.ktor.client.engine.okhttp.OkHttp

actual fun createWeatherRepository(): WeatherRepository {
    val client = HttpClientFactory.create(engine = OkHttp.create())
    val api = KtorWeatherApi(client)
    return WeatherRepositoryImpl(api = api, cacheStore = createWeatherCacheStore())
}
