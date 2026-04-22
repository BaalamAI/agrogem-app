package com.agrogem.app.ui.screens.dashboard

import com.agrogem.app.ui.components.Severity

enum class DashboardSeverity {
    Optimo,
    Atencion,
    Critica,
}

fun DashboardSeverity.toShared(): Severity = when (this) {
    DashboardSeverity.Optimo -> Severity.Optimo
    DashboardSeverity.Atencion -> Severity.Atencion
    DashboardSeverity.Critica -> Severity.Critica
}
