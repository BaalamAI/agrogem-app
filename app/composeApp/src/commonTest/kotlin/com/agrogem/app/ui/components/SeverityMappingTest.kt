package com.agrogem.app.ui.components

import com.agrogem.app.ui.screens.dashboard.DashboardSeverity
import com.agrogem.app.ui.screens.dashboard.toShared
import com.agrogem.app.ui.screens.map.RiskSeverity
import com.agrogem.app.ui.screens.map.toShared
import kotlin.test.Test
import kotlin.test.assertEquals

class SeverityMappingTest {

    @Test
    fun `dashboard severity maps to shared severity`() {
        assertEquals(Severity.Optimo, DashboardSeverity.Optimo.toShared())
        assertEquals(Severity.Atencion, DashboardSeverity.Atencion.toShared())
        assertEquals(Severity.Critica, DashboardSeverity.Critica.toShared())
    }

    @Test
    fun `risk severity maps to shared severity`() {
        assertEquals(Severity.Optimo, RiskSeverity.Optimo.toShared())
        assertEquals(Severity.Atencion, RiskSeverity.Atencion.toShared())
        assertEquals(Severity.Critica, RiskSeverity.Critica.toShared())
    }
}
