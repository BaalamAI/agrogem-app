package com.agrogem.app.data.risk.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Defensive deserialization: fields are nullable with default null so that
 * missing or malformed backend responses do not crash during decoding.
 * Safe defaults are applied in the repository mapping layer (mapDiseaseDto).
 * This mirrors the established WeatherResponse pattern in the project.
 */
@Serializable
data class DiseaseRiskResponse(
    @SerialName("disease")
    val disease: String? = null,
    @SerialName("risk_score")
    val riskScore: Double? = null,
    @SerialName("risk_level")
    val riskLevel: String? = null,
    @SerialName("factors")
    val factors: JsonElement? = null,
    @SerialName("interpretation")
    val interpretation: String? = null,
)

/**
 * Defensive deserialization: fields are nullable with default null so that
 * missing or malformed backend responses do not crash during decoding.
 * Safe defaults are applied in the repository mapping layer (mapPestDto).
 * virus_coalert is explicitly nullable per the backend contract.
 */
@Serializable
data class PestRiskResponse(
    @SerialName("pest")
    val pest: String? = null,
    @SerialName("pest_type")
    val pestType: String? = null,
    @SerialName("life_stage_risk")
    val lifeStageRisk: JsonElement? = null,
    @SerialName("affected_crops")
    val affectedCrops: List<String>? = null,
    @SerialName("risk_score")
    val riskScore: Double? = null,
    @SerialName("risk_level")
    val riskLevel: String? = null,
    @SerialName("virus_coalert")
    val virusCoalert: String? = null,
    @SerialName("factors")
    val factors: JsonElement? = null,
    @SerialName("interpretation")
    val interpretation: String? = null,
)
