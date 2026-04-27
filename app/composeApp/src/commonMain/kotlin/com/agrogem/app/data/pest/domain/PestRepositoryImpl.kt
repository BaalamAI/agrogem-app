package com.agrogem.app.data.pest.domain

import com.agrogem.app.data.ImageResult
import com.agrogem.app.data.network.ApiError
import com.agrogem.app.data.pest.api.PestApi
import com.agrogem.app.data.pest.api.PestIdentifyResponse
import com.agrogem.app.ui.screens.analysis.DiagnosisResult

class PestRepositoryImpl(private val api: PestApi) : PestRepository {

    override suspend fun identify(image: ImageResult): PestResult {
        val bytes = image.bytes
            ?: return PestResult.Failure(PestFailure.MissingImageBytes)

        return try {
            val uploadUrlResponse = try {
                api.getUploadUrl()
            } catch (e: ApiError) {
                logFailure(step = "getUploadUrl", error = e)
                return PestResult.Failure(mapApiError(step = "getUploadUrl", error = e))
            }

            try {
                api.uploadImage(uploadUrlResponse.signedUrl, bytes)
            } catch (e: ApiError) {
                logFailure(step = "uploadImage", error = e)
                return PestResult.Failure(mapApiError(step = "uploadImage", error = e))
            }

            val identifyResponse = try {
                api.identify(uploadUrlResponse.objectPath)
            } catch (e: ApiError) {
                logFailure(step = "identify", error = e)
                return PestResult.Failure(mapApiError(step = "identify", error = e))
            }

            mapIdentifyResponse(identifyResponse)
        } catch (e: ApiError) {
            mapApiError(e)
        } catch (e: Exception) {
            println("[PestRepository] unexpected failure: ${e::class.simpleName}: ${e.message}")
            PestResult.Failure(PestFailure.Network(e))
        }
    }

    private fun mapIdentifyResponse(response: PestIdentifyResponse): PestResult {
        val topMatch = response.topMatch
            ?: return PestResult.Failure(PestFailure.NoMatchFound)

        val pestName = topMatch.pestName.replace("_", " ")
        val confidence = topMatch.similarity.toFloat()
        val severity = deriveSeverity(topMatch.confidence)

        return PestResult.Success(
            DiagnosisResult(
                pestName = pestName,
                confidence = confidence,
                severity = severity,
                affectedArea = "Plagas detectadas",
                cause = pestName,
                diagnosisText = "Se ha detectado $pestName en el cultivo. Nivel de confianza: ${(confidence * 100).toInt()}%. Revisá el área afectada y seguí el plan de tratamiento sugerido.",
                treatmentSteps = listOf(
                    "Aplicar control específico contra $pestName.",
                    "Monitorear el área afectada cada 48 horas.",
                    "Consultar con un especialista si la plaga persiste.",
                ),
            )
        )
    }

    private fun deriveSeverity(confidenceLabel: String): String = when (confidenceLabel.lowercase()) {
        "high" -> "Alta"
        "medium" -> "Media"
        "low" -> "Baja"
        else -> "Media"
    }

    private fun mapApiError(error: ApiError): PestResult = PestResult.Failure(
        mapApiError(step = "unknown", error = error)
    )

    private fun mapApiError(step: String, error: ApiError): PestFailure = when (error) {
        is ApiError.Unauthorized -> PestFailure.ExpiredUrl
        is ApiError.NetworkError -> PestFailure.Network(error.cause)
        is ApiError.NotFound -> {
            if (step == "identify") PestFailure.NoMatchFound else PestFailure.UploadFailed
        }
        is ApiError.Validation -> PestFailure.UploadFailed
        else -> PestFailure.Server
    }

    private fun logFailure(step: String, error: ApiError) {
        println("[PestRepository] step=$step failed with ${error::class.simpleName}: ${error.message}")
    }
}
