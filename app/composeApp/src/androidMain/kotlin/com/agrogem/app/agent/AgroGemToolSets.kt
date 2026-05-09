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
private const val NETWORK_TOOL_TIMEOUT_MS = 15_000L

actual fun createAgroGemToolBundle(tracker: ToolCallTracker): GemmaToolBundle? = GemmaToolBundle(
    tools = listOf(
        WeatherToolSet(tracker = tracker),
        SoilToolSet(tracker = tracker),
        PestRiskToolSet(tracker = tracker),
        LocationLookupToolSet(tracker = tracker),
        LocationGpsToolSet(tracker = tracker),
    ),
    automaticToolCalling = true,
)

class LocationLookupToolSet(
    private val tracker: ToolCallTracker,
    private val geo: GeolocationRepository = createGeolocationRepository(),
) : ToolSet {

    @Tool(description = "Resuelve un nombre de lugar en texto libre (ej. 'Ciudad de Guatemala, Guatemala' o 'Córdoba, Argentina') a coordenadas y lo guarda como la ubicación actual del usuario. Llamá esta herramienta cuando el GPS no esté disponible y el usuario haya indicado dónde está. Después de que retorne exitosamente, podés llamar get_current_weather, get_soil_profile o get_pest_risks.")
    fun resolveLocationByName(query: String): Map<String, Any> = runBlocking(Dispatchers.IO) {
        tracker.markCalled("Ubicación")
        if (query.isBlank()) return@runBlocking errorMap("Falta el nombre del lugar.")
        Log.i(TAG, "geocode tool — query=$query")
        val result = withTimeoutOrNull(NETWORK_TOOL_TIMEOUT_MS) { geo.geocode(query) }
            ?: return@runBlocking errorMap("La consulta de geocodificación tardó más de 15 segundos. Decile al usuario que la red está lenta y que reintente.")
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
    private val tracker: ToolCallTracker,
    private val geo: GeolocationRepository = createGeolocationRepository(),
    private val locationProviderFactory: () -> DeviceLocationProvider = { createDeviceLocationProvider() },
) : ToolSet {

    @Tool(description = "Pide permiso de GPS al usuario y resuelve su ubicación actual precisa mediante el GPS del dispositivo. Preferí esta herramienta sobre pedirle al usuario que escriba un lugar cuando pregunte algo relacionado con la ubicación (clima, suelo, plagas) y aún no haya una ubicación configurada. El sistema mostrará un diálogo de permiso al usuario. Después de que retorne exitosamente, podés llamar get_current_weather, get_soil_profile o get_pest_risks. Si el usuario deniega el permiso o el GPS no está disponible, pedile su municipio y país y llamá resolve_location_by_name.")
    fun requestUserLocation(): Map<String, Any> = runBlocking(Dispatchers.IO) {
        tracker.markCalled("Ubicación GPS")
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
        val reverseResult = withTimeoutOrNull(NETWORK_TOOL_TIMEOUT_MS) { geo.reverseGeocode(latLng) }
            ?: return@runBlocking errorMap("Se obtuvo GPS (lat=${latLng.latitude}, lng=${latLng.longitude}) pero la red está lenta para resolver el nombre del lugar. Pedile al usuario el nombre del municipio o usá las coordenadas si son útiles.")
        reverseResult.fold(
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
    private val tracker: ToolCallTracker,
    private val weather: WeatherRepository = createWeatherRepository(),
    private val geo: GeolocationRepository = createGeolocationRepository(),
) : ToolSet {

    @Tool(description = "Retorna el clima actual (temperatura, humedad, precipitación, viento, UV, condiciones) en la ubicación resuelta del usuario. No requiere argumentos.")
    fun getCurrentWeather(): Map<String, Any> = runBlocking(Dispatchers.IO) {
        tracker.markCalled("Clima")
        val location = geo.currentOrNull()
            ?: return@runBlocking errorMap("La ubicación del usuario no está disponible. Pídele al usuario su ciudad o municipio.")
        Log.i(TAG, "weather tool — location=${location.display.primary}")
        val result = withTimeoutOrNull(NETWORK_TOOL_TIMEOUT_MS) { weather.getCurrentWeather(location.coordinates) }
            ?: return@runBlocking errorMap("La consulta de clima tardó más de 15 segundos. Decile al usuario que la red está lenta y que reintente en un momento.")
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
    private val tracker: ToolCallTracker,
    private val soil: SoilRepository = createSoilRepository(),
    private val geo: GeolocationRepository = createGeolocationRepository(),
) : ToolSet {

    @Tool(description = "Retorna el perfil de suelo (textura dominante, pH, porcentajes de arcilla/arena/limo, interpretación) en la ubicación resuelta del usuario. No requiere argumentos.")
    fun getSoilProfile(): Map<String, Any> = runBlocking(Dispatchers.IO) {
        tracker.markCalled("Suelo")
        val location = geo.currentOrNull()
            ?: return@runBlocking errorMap("La ubicación del usuario no está disponible.")
        Log.i(TAG, "soil tool — location=${location.display.primary}")
        val result = withTimeoutOrNull(NETWORK_TOOL_TIMEOUT_MS) { soil.getSoil(location.coordinates) }
            ?: return@runBlocking errorMap("La consulta de suelo tardó más de 15 segundos. Decile al usuario que la red está lenta y que reintente en un momento.")
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
    private val tracker: ToolCallTracker,
    private val risk: RiskRepository = createRiskRepository(),
    private val geo: GeolocationRepository = createGeolocationRepository(),
) : ToolSet {

    @Tool(description = "Retorna los niveles actuales de riesgo de plagas agrícolas (insectos y artrópodos) para la ubicación resuelta del usuario. Cada entrada incluye nombre, severidad (Optimo/Atencion/Critica), puntaje e interpretación. No requiere argumentos.")
    fun getPestRisks(): Map<String, Any> = runBlocking(Dispatchers.IO) {
        tracker.markCalled("Plagas")
        val location = geo.currentOrNull()
            ?: return@runBlocking errorMap("La ubicación del usuario no está disponible.")
        Log.i(TAG, "pest tool — location=${location.display.primary}")
        val result = withTimeoutOrNull(NETWORK_TOOL_TIMEOUT_MS) { risk.getPestRisks(location.coordinates) }
            ?: return@runBlocking errorMap("La consulta de plagas tardó más de 15 segundos. Decile al usuario que la red está lenta y que reintente en un momento.")
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
