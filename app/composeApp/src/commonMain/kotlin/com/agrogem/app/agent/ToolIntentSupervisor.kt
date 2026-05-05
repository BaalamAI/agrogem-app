package com.agrogem.app.agent

object ToolIntentSupervisor {
    const val WEATHER_TOOL = "Clima"
    const val SOIL_TOOL = "Suelo"
    const val PEST_TOOL = "Plagas"
    const val LOCATION_TOOL = "Ubicación"
    const val GPS_LOCATION_TOOL = "Ubicación GPS"

    private val weatherKeywords = listOf(
        "clima",
        "lluvia",
        "llover",
        "temperatura",
        "humedad",
        "viento",
        "precipitacion",
        "uv",
    )

    private val soilKeywords = listOf(
        "suelo",
        "ph",
        "textura",
        "arcilla",
        "arena",
        "limo",
        "materia organica",
    )

    private val pestKeywords = listOf(
        "plaga",
        "plagas",
        "insecto",
        "insectos",
        "riesgo de plaga",
        "riesgo de plagas",
        "presion de plaga",
        "presion de plagas",
    )

    private val locationKeywords = listOf(
        "ubicacion",
        "ubicación",
        "gps",
        "donde estoy",
        "mi zona",
        "mi finca",
    )

    fun expectedToolsFor(text: String): Set<String> {
        val normalized = text.normalizedForIntent()
        return buildSet {
            if (weatherKeywords.any { normalized.contains(it) }) add(WEATHER_TOOL)
            if (soilKeywords.any { normalized.contains(it) }) add(SOIL_TOOL)
            if (pestKeywords.any { normalized.contains(it) }) add(PEST_TOOL)
            if (locationKeywords.any { normalized.contains(it) }) add(LOCATION_TOOL)
        }
    }

    fun shouldRetry(expectedTools: Set<String>, calledTools: Set<String>): Boolean {
        if (expectedTools.isEmpty()) return false
        return expectedTools.intersect(calledTools).isEmpty()
    }

    fun buildRetryPrompt(originalText: String, expectedTools: Set<String>): String {
        val expected = expectedTools.joinToString(", ")
        return buildString {
            appendLine("Esta pregunta requiere datos reales de herramientas ($expected).")
            appendLine("Antes de responder, llamá la herramienta correspondiente.")
            appendLine("Si falta ubicación, primero llamá request_user_location; si no funciona, pedí municipio y país o usá resolve_location_by_name si el usuario ya dio el lugar.")
            appendLine()
            append(originalText)
        }
    }
}

private fun String.normalizedForIntent(): String =
    lowercase()
        .replace("á", "a")
        .replace("é", "e")
        .replace("í", "i")
        .replace("ó", "o")
        .replace("ú", "u")
        .replace("ü", "u")
