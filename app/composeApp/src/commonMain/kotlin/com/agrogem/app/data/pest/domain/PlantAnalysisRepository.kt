package com.agrogem.app.data.pest.domain

import com.agrogem.app.data.GemmaManager
import com.agrogem.app.data.GemmaPreparation
import com.agrogem.app.data.ImageResult
import com.agrogem.app.data.connectivity.ConnectivityMonitor
import kotlin.math.round
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

interface PlantAnalysisRepository {
    suspend fun analyze(image: ImageResult): PestResult
}

class PlantAnalysisRepositoryImpl(
    private val gemmaManager: GemmaManager,
    private val gemmaPreparation: GemmaPreparation,
    private val pestRepository: PestRepository,
    private val connectivityMonitor: ConnectivityMonitor,
) : PlantAnalysisRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun analyze(image: ImageResult): PestResult {
        val isOnline = connectivityMonitor.isOnline()

        val backendResult = if (isOnline) {
            runCatching { pestRepository.identify(image) }.getOrNull()
        } else {
            null
        }

        val shouldAttemptGemma = !isOnline || gemmaPreparation.hasLocalModel()
        if (!shouldAttemptGemma) {
            return backendResult
                ?: PestResult.Failure(PestFailure.Network(Exception("No local model available for offline analysis")))
        }

        val gemmaReady = initializeGemmaIfPossible()

        if (!gemmaReady) {
            return backendResult
                ?: PestResult.Failure(PestFailure.Network(Exception("No internet connection and Gemma model not available")))
        }

        val backendSuccess = backendResult as? PestResult.Success

        val gemmaResult = runCatching {
            val backendConfidence = backendSuccess?.diagnosis?.confidence
            val responseText = gemmaManager.sendMessage(
                systemPrompt = buildSystemPrompt(backendSuccess),
                userPrompt = USER_PROMPT,
                images = listOfNotNull(image.uri),
                temperature = 0.4f,
            )
            parseGemmaResponse(responseText, backendConfidence)
        }.getOrNull()

        return gemmaResult ?: backendResult ?: PestResult.Failure(PestFailure.Server)
    }

    private suspend fun initializeGemmaIfPossible(): Boolean = gemmaPreparation.ensureReady()

    private fun buildSystemPrompt(backendResult: PestResult.Success?): String = buildString {
        append("Eres AgroGem, un experto en diagnóstico visual de salud de cultivos. ")
        append("Analiza la imagen del cultivo buscando enfermedades, hongos, bacterias, virus, insectos, ácaros, nematodos, plagas, deficiencias nutricionales o estrés ambiental. ")
        append("La respuesta debe servir para orientar una decisión agrícola práctica. ")
        append("Responde EXCLUSIVAMENTE con un objeto JSON válido, ")
        append("sin texto adicional ni marcadores de código. ")
        append("El JSON debe tener exactamente estos campos: ")
        append("\"pestName\" (string corto con la enfermedad, plaga o daño probable, en español, sin paréntesis), ")
        append("\"confidence\" (número decimal entre 0.0 y 1.0), ")
        append("\"severity\" (string: Alta, Media o Baja), ")
        append("\"affectedArea\" (string), ")
        append("\"cause\" (string: hongo, bacteria, virus, insecto, ácaro, nematodo, deficiencia, estrés ambiental o desconocida), ")
        append("\"diagnosisText\" (string: descripción detallada con síntomas visibles y diagnóstico diferencial si aplica), ")
        append("\"treatmentSteps\" (array de strings). ")
        append("En treatmentSteps incluye acciones de manejo agrícola concretas y seguras: aislar/retirar tejido afectado cuando aplique, monitorear, mejorar ventilación/drenaje/nutrición y consultar etiqueta o técnico antes de agroquímicos. ")
        append("Si no puedes inferir con suficiente certeza el área afectada, la causa o el diagnóstico, devuelve una cadena vacía en esos campos y baja la confidence. ")
        append("La imagen es la fuente principal de verdad; cualquier contexto adicional es solo referencia para enriquecer, no para forzar una clase. ")
        if (backendResult != null) {
            backendResult.evidence?.let { evidence ->
                append("Contexto adicional del sistema de reconocimiento: ")
                append("plaga sugerida '${evidence.topMatchName.toSpanishDisplayPestName()}', ")
                append("similitud ${(evidence.similarity * 100).toInt()}%, ")
                append("peso ${(evidence.weightedScore * 100).toInt()}%, ")
                append("nivel '${evidence.confidenceLabel}'. ")
                if (evidence.alternatives.isNotEmpty()) {
                    append("Alternativas consideradas: ")
                    append(
                        evidence.alternatives.joinToString(separator = "; ") { alternative ->
                            "${alternative.pestName.toSpanishDisplayPestName()} (${(alternative.similarity * 100).toInt()}%)"
                        },
                    )
                    append(". ")
                }
                if (evidence.votes.isNotEmpty()) {
                    append("Votos ponderados por clase: ")
                    append(
                        evidence.votes.entries.joinToString(separator = "; ") { (name, score) ->
                            "${name.toSpanishDisplayPestName()}=${score.toRoundedString()}"
                        },
                    )
                    append(". ")
                }
            }
        }
    }

    private fun parseGemmaResponse(raw: String, backendConfidence: Float?): PestResult? {
        val cleaned = extractJson(raw) ?: return null

        return try {
            val parsed = json.decodeFromString<GemmaAnalysisJson>(cleaned)
            if (parsed.pestName.isBlank()) return null
            val displayName = parsed.pestName.toSpanishDisplayPestName()
            PestResult.Success(
                AnalysisDiagnosis(
                    pestName = displayName,
                    confidence = backendConfidence ?: parsed.confidence.coerceIn(0f, 1f),
                    severity = normalizeSeverity(parsed.severity),
                    affectedArea = parsed.affectedArea.trim(),
                    cause = parsed.cause.trim(),
                    diagnosisText = parsed.diagnosisText,
                    treatmentSteps = parsed.treatmentSteps,
                    isConfidenceReliable = backendConfidence != null,
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

    private fun String.toSpanishDisplayPestName(): String {
        val cleaned = trim().substringBefore("(").trim().replace("_", " ")
        val normalized = cleaned.lowercase()
        fun hasWord(word: String): Boolean = Regex("\\b${Regex.escape(word)}\\b").containsMatchIn(normalized)
        val translated = when {
            hasWord("mildew") || hasWord("oidium") || hasWord("oídio") -> "Mildiu"
            hasWord("aphid") -> "Pulgones"
            hasWord("whitefly") -> "Mosca blanca"
            hasWord("rust") -> "Roya"
            hasWord("blight") -> "Tizón"
            hasWord("spot") -> "Mancha foliar"
            else -> cleaned
        }

        return translated
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ") { token -> token.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
    }

    private fun Float.toRoundedString(): String = ((round(this * 100) / 100f).toString())

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
