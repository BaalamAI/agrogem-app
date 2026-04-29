package com.agrogem.app.data.pest.domain

import androidx.compose.runtime.Immutable

@Immutable
data class AnalysisDiagnosis(
    val pestName: String,
    val confidence: Float,
    val severity: String,
    val affectedArea: String,
    val cause: String,
    val diagnosisText: String,
    val treatmentSteps: List<String>,
    val isConfidenceReliable: Boolean = false,
)
