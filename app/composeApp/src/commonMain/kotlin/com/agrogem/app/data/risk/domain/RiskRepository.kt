package com.agrogem.app.data.risk.domain

import com.agrogem.app.data.network.ApiError
import com.agrogem.app.data.risk.api.RiskApi
import com.agrogem.app.data.shared.domain.LatLng

interface RiskRepository {
    suspend fun getDiseaseRisks(latLng: LatLng?): Result<List<DiseaseRisk>>
    suspend fun getPestRisks(latLng: LatLng?): Result<List<DiseaseRisk>>
}

class RiskRepositoryImpl(
    private val api: RiskApi,
) : RiskRepository {

    override suspend fun getDiseaseRisks(latLng: LatLng?): Result<List<DiseaseRisk>> {
        if (latLng == null) {
            return Result.failure(IllegalStateException("Sin ubicación"))
        }
        return try {
            val results = DEFAULT_DISEASES.map { disease ->
                val dto = api.getDiseaseRisk(latLng.latitude, latLng.longitude, disease)
                mapDiseaseDto(disease, dto)
            }
            Result.success(results)
        } catch (e: ApiError) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPestRisks(latLng: LatLng?): Result<List<DiseaseRisk>> {
        if (latLng == null) {
            return Result.failure(IllegalStateException("Sin ubicación"))
        }
        return try {
            val results = DEFAULT_PESTS.map { pest ->
                val dto = api.getPestRisk(latLng.latitude, latLng.longitude, pest)
                mapPestDto(pest, dto)
            }
            Result.success(results)
        } catch (e: ApiError) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

internal fun mapDiseaseDto(diseaseKey: String, dto: com.agrogem.app.data.risk.api.DiseaseRiskResponse): DiseaseRisk {
    val level = dto.riskLevel?.lowercase() ?: ""
    val severity = when (level) {
        "low", "very_low", "none" -> RiskSeverity.Optimo
        "moderate", "medium" -> RiskSeverity.Atencion
        "high", "very_high" -> RiskSeverity.Critica
        else -> RiskSeverity.Atencion
    }
    return DiseaseRisk(
        diseaseName = dto.disease ?: diseaseKey,
        displayName = mapDiseaseBackend(diseaseKey),
        score = dto.riskScore ?: 0.0,
        severity = severity,
        interpretation = dto.interpretation ?: "",
        factors = dto.factors ?: emptyList(),
    )
}

internal fun mapPestDto(pestKey: String, dto: com.agrogem.app.data.risk.api.PestRiskResponse): DiseaseRisk {
    val level = dto.riskLevel?.lowercase() ?: ""
    val severity = when (level) {
        "low", "very_low", "none" -> RiskSeverity.Optimo
        "moderate", "medium" -> RiskSeverity.Atencion
        "high", "very_high" -> RiskSeverity.Critica
        else -> RiskSeverity.Atencion
    }
    return DiseaseRisk(
        diseaseName = dto.pest ?: pestKey,
        displayName = mapDiseaseBackend(pestKey),
        score = dto.riskScore ?: 0.0,
        severity = severity,
        interpretation = dto.interpretation ?: "",
        factors = dto.factors ?: emptyList(),
    )
}

fun mapDiseaseBackend(disease: String): String = when (disease) {
    "coffee_rust" -> "Roya del café"
    "late_blight" -> "Tizón tardío"
    "corn_rust" -> "Roya del maíz"
    else -> disease.replace("_", " ").replaceFirstChar { it.uppercase() }
}

private val DEFAULT_DISEASES = listOf("coffee_rust", "late_blight", "corn_rust")
private val DEFAULT_PESTS = listOf("spider_mite")
