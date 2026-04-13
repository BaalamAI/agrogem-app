package com.agrogem.app.navigation

sealed interface AgroGemRoute {
    val route: String
    val title: String

    data object Dashboard : AgroGemRoute {
        override val route: String = "dashboard"
        override val title: String = "Dashboard"
    }

    data object Camera : AgroGemRoute {
        override val route: String = "camera"
        override val title: String = "Cámara"
    }

    data object Analysis : AgroGemRoute {
        override val route: String = "analysis"
        override val title: String = "Análisis"
    }

    data object Map : AgroGemRoute {
        override val route: String = "map"
        override val title: String = "Mapa"
    }

    data object Report : AgroGemRoute {
        override val route: String = "report"
        override val title: String = "Reporte"
    }

    companion object {
        val all = listOf(Dashboard, Camera, Map, Analysis, Report)

        fun fromRoute(route: String?): AgroGemRoute =
            all.firstOrNull { it.route == route } ?: Dashboard
    }
}
