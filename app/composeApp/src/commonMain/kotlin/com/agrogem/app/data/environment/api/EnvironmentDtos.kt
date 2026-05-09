package com.agrogem.app.data.environment.api

import com.agrogem.app.data.soil.api.SoilResponse
import com.agrogem.app.data.weather.api.WeatherResponse
import kotlinx.serialization.Serializable

@Serializable
data class EnvironmentResponseDto(
    val lat: Double? = null,
    val lon: Double? = null,
    val weather: WeatherResponse? = null,
    val soil: SoilResponse? = null,
)
