package com.agrogem.app.data.risk.domain

import androidx.compose.runtime.Immutable

@Immutable
data class DiseaseRisk(
    val diseaseName: String,
    val displayName: String,
    val score: Double,
    val severity: RiskSeverity,
    val interpretation: String,
    val factors: List<String>,
)
