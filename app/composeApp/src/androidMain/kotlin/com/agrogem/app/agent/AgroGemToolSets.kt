package com.agrogem.app.agent

import android.util.Log
import com.agrogem.app.data.GemmaToolBundle
import com.agrogem.app.data.geolocation.createGeolocationRepository
import com.agrogem.app.data.geolocation.domain.GeolocationRepository
import com.agrogem.app.data.geolocation.domain.ResolvedLocation
import com.agrogem.app.data.location.DeviceLocationProvider
import com.agrogem.app.data.location.LocationGate
import com.agrogem.app.data.location.createDeviceLocationProvider
import com.agrogem.app.data.network.ApiError
import com.agrogem.app.data.risk.createRiskRepository
import com.agrogem.app.data.risk.domain.RiskRepository
import com.agrogem.app.data.soil.createSoilRepository
import com.agrogem.app.data.soil.domain.SoilRepository
import com.agrogem.app.data.weather.createWeatherRepository
import com.agrogem.app.data.weather.domain.WeatherRepository
import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "AgroGemTools"

actual fun createAgroGemToolBundle(): GemmaToolBundle? = GemmaToolBundle(
    tools = listOf(
        WeatherToolSet(),
        SoilToolSet(),
        PestRiskToolSet(),
        LocationLookupToolSet(),
        LocationGpsToolSet(),
    ),
    automaticToolCalling = true,
)

class LocationLookupToolSet(
    private val geo: GeolocationRepository = createGeolocationRepository(),
) : ToolSet {

    @Tool(description = "Resolves a free-text place name (e.g. 'Ciudad de Guatemala, Guatemala' or 'Córdoba, Argentina') to coordinates and saves it as the user's current location. Call this when GPS is unavailable and the user has stated where they are. After this returns successfully, you can call get_current_weather, get_soil_profile, or get_pest_risks.")
    fun resolveLocationByName(query: String): Map<String, Any> = runBlocking(Dispatchers.IO) {
        toolCallTracker.markCalled("Ubicación")
        if (query.isBlank()) return@runBlocking errorMap("Falta el nombre del lugar.")
        Log.i(TAG, "geocode tool — query=$query")
        val result = geo.geocode(query)
        result.fold(
            onSuccess = { resolved ->
                val loc = resolved.location
                Log.i(TAG, "geocode tool — OK: location=${loc.display.primary} lat=${loc.coordinates.latitude} lng=${loc.coordinates.longitude}")
                buildMap<String, Any> {
                    put("location", loc.display.primary)
                    put("lat", loc.coordinates.latitude)
                    put("lng", loc.coordinates.longitude)
                    loc.display.municipality?.let { put("municipality", it) }
                    loc.display.state?.let { put("state", it) }
                    loc.display.country?.let { put("country", it) }
                    resolved.interpretation?.let { put("interpretation", it) }
                }
            },
            onFailure = { err ->
                Log.w(TAG, "geocode tool — FAIL for '$query': ${err::class.simpleName}: ${err.message}", err)
                val msg = when (err) {
                    is NoSuchElementException, is ApiError.NotFound ->
                        "Sin resultados para '$query'. Probablemente hay un error de ortografía o falta el país. Pedile al usuario que verifique cómo escribió el lugar y volvé a llamar resolve_location_by_name con la corrección."
                    is ApiError.NetworkError ->
                        "No hay conexión para resolver '$query'. Decile al usuario que necesitás internet para esta consulta."
                    else ->
                        "Falló la resolución de '$query' (${err::class.simpleName}: ${err.message}). Pedile al usuario que reintente con otro nombre."
                }
                errorMap(msg)
            },
        )
    }
}

class LocationGpsToolSet(
    private val geo: GeolocationRepository = createGeolocationRepository(),
    private val locationProviderFactory: () -> DeviceLocationProvider = { createDeviceLocationProvider() },
) : ToolSet {

    @Tool(description = "Asks the user for GPS permission and resolves their precise current location via the device's GPS. Prefer this over asking the user to type a place name when they ask anything location-specific (clima, suelo, plagas) and no location is set yet. The system will show a permission dialog to the user. After this returns successfully, you can call get_current_weather, get_soil_profile, or get_pest_risks. If the user denies permission or GPS is unavailable, fall back to asking them for their municipality and country and call resolve_location_by_name.")
    fun requestUserLocation(): Map<String, Any> = runBlocking(Dispatchers.IO) {
        toolCallTracker.markCalled("Ubicación GPS")
        Log.i(TAG, "request location tool — start")
        val granted = withTimeoutOrNull(60_000L) { LocationGate.requestPermission() }
        if (granted != true) {
            val reason = if (granted == null) "el usuario no respondió al diálogo" else "el usuario denegó el permiso"
            Log.w(TAG, "request location tool — permission $reason")
            return@runBlocking errorMap("No se obtuvo permiso de ubicación ($reason). Pedile al usuario su municipio y país, y luego llamá resolve_location_by_name con esa cadena.")
        }
        val provider = runCatching { locationProviderFactory() }.getOrNull()
            ?: return@runBlocking errorMap("No se pudo crear el proveedor de GPS. Pedile al usuario su municipio y país y llamá resolve_location_by_name.")
        val latLng = withTimeoutOrNull(30_000L) { provider.getCurrentLatLng() }?.getOrNull()
        if (latLng == null) {
            Log.w(TAG, "request location tool — GPS no resolvió en 30s")
            return@runBlocking errorMap("No se pudo obtener una lectura de GPS (probablemente está apagado o sin señal). Pedile al usuario que active el GPS o que escriba su ubicación, y luego llamá resolve_location_by_name.")
        }
        geo.reverseGeocode(latLng).fold(
            onSuccess = { resolved ->
                Log.i(TAG, "request location tool — OK: ${resolved.display.primary} (${latLng.latitude}, ${latLng.longitude})")
                buildMap<String, Any> {
                    put("location", resolved.display.primary)
                    put("lat", resolved.coordinates.latitude)
                    put("lng", resolved.coordinates.longitude)
                    resolved.display.municipality?.let { put("municipality", it) }
                    resolved.display.state?.let { put("state", it) }
                    resolved.display.country?.let { put("country", it) }
                }
            },
            onFailure = { err ->
                Log.w(TAG, "request location tool — reverseGeocode FAIL: ${err::class.simpleName}: ${err.message}", err)
                errorMap("Se obtuvo GPS pero no se pudo resolver el nombre del lugar (${err::class.simpleName}: ${err.message}). Las coordenadas son lat=${latLng.latitude}, lng=${latLng.longitude}; podés usarlas o pedirle al usuario el nombre del lugar.")
            },
        )
    }
}

class WeatherToolSet(
    private val weather: WeatherRepository = createWeatherRepository(),
    private val geo: GeolocationRepository = createGeolocationRepository(),
) : ToolSet {

    @Tool(description = "Returns the current weather (temperature, humidity, precipitation, wind, UV, conditions) at the user's current resolved location. No arguments needed.")
    fun getCurrentWeather(): Map<String, Any> = runBlocking(Dispatchers.IO) {
        toolCallTracker.markCalled("Clima")
        val location = geo.currentOrNull()
            ?: return@runBlocking errorMap("La ubicación del usuario no está disponible. Pídele al usuario su ciudad o municipio.")
        Log.i(TAG, "weather tool — location=${location.display.primary}")
        val result = weather.getCurrentWeather(location.coordinates)
        result.fold(
            onSuccess = { w ->
                Log.i(TAG, "weather tool — OK: temp=${w.temperatureCelsius} interpretation=${w.interpretation?.take(80)}")
                buildMap<String, Any> {
                    put("location", location.display.primary)
                    put("temperature", w.temperatureCelsius)
                    put("humidity", w.humidity)
                    put("precipitation", w.precipitation)
                    put("wind_speed", w.windSpeed)
                    put("max_min", w.maxMin)
                    put("uv_index", w.uvIndex)
                    put("description", w.description)
                    w.interpretation?.let { put("interpretation", it) }
                }
            },
            onFailure = { err ->
                Log.w(TAG, "weather tool — FAIL: ${err::class.simpleName}: ${err.message}", err)
                val msg = when (err) {
                    is ApiError.NotFound ->
                        "No hay datos de clima disponibles para esta ubicación. Decile al usuario que la fuente de datos no cubre ese punto."
                    is ApiError.NetworkError ->
                        "No hay conexión para consultar el clima. Decile al usuario que necesitás internet."
                    else ->
                        "Falló la consulta de clima (${err::class.simpleName}: ${err.message}). Decile al usuario que el servicio está fallando y que reintente más tarde."
                }
                errorMap(msg)
            },
        )
    }
}

class SoilToolSet(
    private val soil: SoilRepository = createSoilRepository(),
    private val geo: GeolocationRepository = createGeolocationRepository(),
) : ToolSet {

    @Tool(description = "Returns the soil profile (dominant texture, pH, clay/sand/silt percentages, interpretation) at the user's current resolved location. No arguments needed.")
    fun getSoilProfile(): Map<String, Any> = runBlocking(Dispatchers.IO) {
        toolCallTracker.markCalled("Suelo")
        val location = geo.currentOrNull()
            ?: return@runBlocking errorMap("La ubicación del usuario no está disponible.")
        Log.i(TAG, "soil tool — location=${location.display.primary}")
        val result = soil.getSoil(location.coordinates)
        result.fold(
            onSuccess = { profile ->
                val top = profile.domainHorizons.firstOrNull()
                Log.i(TAG, "soil tool — OK: texture=${profile.dominantTexture} interpretation=${profile.interpretation.take(80)}")
                buildMap<String, Any> {
                    put("location", location.display.primary)
                    put("dominant_texture", profile.dominantTexture)
                    put("ph_top_horizon", top?.ph?.toString() ?: "n/d")
                    put("clay_percent", top?.clayPct ?: "n/d")
                    put("sand_percent", top?.sandPct ?: "n/d")
                    put("silt_percent", top?.siltPct ?: "n/d")
                    put("soc_g_per_kg", top?.socGPerKg ?: "n/d")
                    if (profile.interpretation.isNotBlank()) {
                        put("interpretation", profile.interpretation)
                    }
                }
            },
            onFailure = { err ->
                Log.w(TAG, "soil tool — FAIL: ${err::class.simpleName}: ${err.message}", err)
                val msg = when (err) {
                    is ApiError.NotFound ->
                        "No hay datos de suelo de SoilGrids para esta ubicación. Esto es habitual en zonas urbanas o pixels enmascarados del modelo. Decile al usuario que para suelo necesitás un punto rural/agrícola y ofrecé contestar lo que puedas con conocimiento general."
                    is ApiError.NetworkError ->
                        "No hay conexión para consultar el suelo. Decile al usuario que necesitás internet."
                    else ->
                        "Falló la consulta de suelo (${err::class.simpleName}: ${err.message}). Decile al usuario que el servicio está fallando y que reintente más tarde."
                }
                errorMap(msg)
            },
        )
    }
}

class PestRiskToolSet(
    private val risk: RiskRepository = createRiskRepository(),
    private val geo: GeolocationRepository = createGeolocationRepository(),
) : ToolSet {

    @Tool(description = "Returns the current agricultural pest risk levels (insects and arthropods) for the user's current resolved location. Each entry includes a name, severity (Optimo/Atencion/Critica), score and interpretation. No arguments needed.")
    fun getPestRisks(): Map<String, Any> = runBlocking(Dispatchers.IO) {
        toolCallTracker.markCalled("Plagas")
        val location = geo.currentOrNull()
            ?: return@runBlocking errorMap("La ubicación del usuario no está disponible.")
        Log.i(TAG, "pest tool — location=${location.display.primary}")
        val result = risk.getPestRisks(location.coordinates)
        result.fold(
            onSuccess = { risks ->
                Log.i(TAG, "pest tool — OK: count=${risks.size} top=${risks.maxByOrNull { it.score }?.let { "${it.displayName}(${it.severity.name})" }}")
                mapOf(
                    "location" to location.display.primary,
                    "pests" to risks.map { r ->
                        buildMap<String, Any> {
                            put("name", r.displayName)
                            put("severity", r.severity.name)
                            put("score", r.score)
                            if (r.interpretation.isNotBlank()) {
                                put("interpretation", r.interpretation)
                            }
                        }
                    },
                )
            },
            onFailure = { err ->
                Log.w(TAG, "pest tool — FAIL: ${err::class.simpleName}: ${err.message}", err)
                val msg = when (err) {
                    is ApiError.NotFound ->
                        "No hay datos de riesgo de plagas para esta ubicación. Decile al usuario que la fuente de datos no cubre ese punto."
                    is ApiError.NetworkError ->
                        "No hay conexión para consultar plagas. Decile al usuario que necesitás internet."
                    else ->
                        "Falló la consulta de plagas (${err::class.simpleName}: ${err.message}). Decile al usuario que el servicio está fallando y que reintente más tarde."
                }
                errorMap(msg)
            },
        )
    }
}

private suspend fun GeolocationRepository.currentOrNull(): ResolvedLocation? =
    runCatching { observeResolvedLocation().first() }.getOrNull()

private fun errorMap(message: String): Map<String, Any> = mapOf("error" to message)
