package com.agrogem.app.data.pest.domain

import com.agrogem.app.data.GemmaManager
import com.agrogem.app.data.GemmaPreparationStateHolder
import com.agrogem.app.data.ImageResult
import com.agrogem.app.data.connectivity.ConnectivityMonitor
import com.agrogem.app.ui.screens.analysis.DiagnosisResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

interface PlantAnalysisRepository {
    suspend fun analyze(image: ImageResult): PestResult
}

class PlantAnalysisRepositoryImpl(
    private val gemmaManager: GemmaManager,
    private val gemmaPreparationStateHolder: GemmaPreparationStateHolder,
    private val pestRepository: PestRepository,
    private val connectivityMonitor: ConnectivityMonitor,
) : PlantAnalysisRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun analyze(image: ImageResult): PestResult {
        val gemmaReady = initializeGemmaIfPossible()
        val isOnline = connectivityMonitor.isOnline()

        val backendResult = if (isOnline) {
            runCatching { pestRepository.identify(image) }.getOrNull()
        } else {
            null
        }

        if (!gemmaReady) {
            return backendResult
                ?: PestResult.Failure(PestFailure.Network(Exception("No internet connection and Gemma model not available")))
        }

        val backendSuccess = backendResult as? PestResult.Success

        val gemmaResult = runCatching {
            val responseText = gemmaManager.sendMessage(
                systemPrompt = buildSystemPrompt(backendSuccess),
                userPrompt = USER_PROMPT,
                images = listOfNotNull(image.uri),
                temperature = 0.4f,
            )
            parseGemmaResponse(responseText)
        }.getOrNull()

        return gemmaResult ?: backendResult ?: PestResult.Failure(PestFailure.Server)
    }

    private suspend fun initializeGemmaIfPossible(): Boolean = gemmaPreparationStateHolder.ensureReady()

    private fun buildSystemPrompt(backendResult: PestResult.Success?): String = buildString {
        append("Eres un experto Fitopatólogo Especialista en enfermedades de cultivos. ")
        append("Analiza la imagen del cultivo y responde EXCLUSIVAMENTE con un objeto JSON válido, ")
        append("sin texto adicional ni marcadores de código. ")
        append("El JSON debe tener exactamente estos campos: ")
        append("\"pestName\" (string), ")
        append("\"confidence\" (número decimal entre 0.0 y 1.0), ")
        append("\"severity\" (string: Alta, Media o Baja), ")
        append("\"affectedArea\" (string), ")
        append("\"cause\" (string), ")
        append("\"diagnosisText\" (string: descripción detallada), ")
        append("\"treatmentSteps\" (array de strings). ")
        if (backendResult != null) {
            append("Contexto adicional del sistema de reconocimiento: plaga sugerida '${backendResult.diagnosis.pestName}' ")
            append("con confianza ${(backendResult.diagnosis.confidence * 100).toInt()}%. ")
            append("Usá esta información solo como referencia; tu análisis es la fuente de verdad.")
        }
    }

    private fun parseGemmaResponse(raw: String): PestResult? {
        val cleaned = extractJson(raw) ?: return null

        return try {
            val parsed = json.decodeFromString<GemmaAnalysisJson>(cleaned)
            if (parsed.pestName.isBlank()) return null
            PestResult.Success(
                DiagnosisResult(
                    pestName = parsed.pestName,
                    confidence = parsed.confidence.coerceIn(0f, 1f),
                    severity = normalizeSeverity(parsed.severity),
                    affectedArea = parsed.affectedArea,
                    cause = parsed.cause,
                    diagnosisText = parsed.diagnosisText,
                    treatmentSteps = parsed.treatmentSteps,
                )
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extracts a JSON object from raw text that may be wrapped in markdown
     * fences or surrounded by prose. Prefers content inside ``` fences, then
     * scans for the first '{' and tracks brace depth to locate the matching
     * closing '}'.
     */
    private fun extractJson(raw: String): String? {
        val candidate = extractFencedJson(raw) ?: raw

        val start = candidate.indexOf('{')
        if (start == -1) return null

        var depth = 0
        var inString = false
        var escape = false
        for (i in start until candidate.length) {
            val c = candidate[i]
            when {
                escape -> escape = false
                c == '\\' -> escape = true
                c == '"' && !escape -> inString = !inString
                !inString -> {
                    when (c) {
                        '{' -> depth++
                        '}' -> {
                            depth--
                            if (depth == 0) return candidate.substring(start, i + 1)
                        }
                    }
                }
            }
        }
        return null
    }

    private fun extractFencedJson(raw: String): String? {
        val start = raw.indexOf("```")
        if (start == -1) return null
        val newlineAfterOpen = raw.indexOf('\n', start)
        val contentStart = if (newlineAfterOpen != -1) newlineAfterOpen + 1 else start + 3
        val close = raw.indexOf("```", contentStart)
        if (close == -1) return null
        return raw.substring(contentStart, close).trim()
    }

    /**
     * Normalizes free-form severity strings into the stable set
     * Alta / Media / Baja expected by the analysis UI.
     */
    private fun normalizeSeverity(raw: String): String = when (raw.trim().lowercase()) {
        "alta", "high", "severe", "critical", "critica", "crítica", "grave" -> "Alta"
        "media", "medium", "moderate", "moderada" -> "Media"
        "baja", "low", "mild", "leve" -> "Baja"
        else -> "Media"
    }

    private companion object {
        const val USER_PROMPT = "Realizá un diagnóstico completo de esta imagen de cultivo."
    }
}

@Serializable
private data class GemmaAnalysisJson(
    val pestName: String = "",
    val confidence: Float = 0.0f,
    val severity: String = "",
    val affectedArea: String = "",
    val cause: String = "",
    val diagnosisText: String = "",
    val treatmentSteps: List<String> = emptyList(),
)
