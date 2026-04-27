package com.agrogem.app.ui.screens.map

import androidx.compose.runtime.Immutable
import com.agrogem.app.data.risk.domain.RiskSeverity
import com.agrogem.app.ui.components.Severity

@Immutable
data class MapRiskUiState(
    val title: String,
    val subtitle: String,
    val riskSummary: List<RiskSummaryItem>,
    val markers: List<RiskMarker>,
    val alerts: List<RiskAlert>,
    val disclaimer: String,
)

@Immutable
data class RiskSummaryItem(
    val label: String,
    val value: String,
)

@Immutable
data class RiskMarker(
    val id: String,
    val lot: String,
    val riskLabel: String,
    val xFraction: Float,
    val yFraction: Float,
    val severity: RiskSeverity,
)

@Immutable
data class RiskAlert(
    val id: String,
    val lot: String,
    val detail: String,
    val recommendation: String,
    val severity: RiskSeverity,
)

fun RiskSeverity.toShared(): Severity = when (this) {
    RiskSeverity.Optimo -> Severity.Optimo
    RiskSeverity.Atencion -> Severity.Atencion
    RiskSeverity.Critica -> Severity.Critica
}
