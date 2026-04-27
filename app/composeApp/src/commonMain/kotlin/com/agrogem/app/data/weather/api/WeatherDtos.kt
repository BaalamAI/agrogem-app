package com.agrogem.app.data.weather.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherResponse(
    @SerialName("temperature_Celsius")
    val temperatureCelsius: Double? = null,
    @SerialName("humidity_Percentage")
    val humidityPercentage: Double? = null,
    @SerialName("cloudCover_Percentage")
    val cloudCoverPercentage: Double? = null,
    @SerialName("uvIndex")
    val uvIndex: Double? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("dateTime")
    val dateTime: String? = null,
)
