package com.agrogem.app.data.risk.api

interface RiskApi {
    suspend fun getDiseaseRisk(lat: Double, lon: Double, disease: String): DiseaseRiskResponse
    suspend fun getPestRisk(lat: Double, lon: Double, pest: String): PestRiskResponse
}
