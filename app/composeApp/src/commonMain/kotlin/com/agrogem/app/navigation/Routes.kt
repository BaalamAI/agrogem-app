package com.agrogem.app.navigation

enum class AgroGemBottomTab {
    Home,
    Fields,
    Scan,
    Maps,
    Profile,
}

sealed interface AgroGemRoute {
    val route: String
    val title: String
    val bottomTab: AgroGemBottomTab?

    data object Home : AgroGemRoute {
        override val route: String = "home"
        override val title: String = "Home"
        override val bottomTab: AgroGemBottomTab = AgroGemBottomTab.Home
    }

    data object Camera : AgroGemRoute {
        override val route: String = "camera"
        override val title: String = "Cámara"
        override val bottomTab: AgroGemBottomTab = AgroGemBottomTab.Scan
    }

    data object Analysis : AgroGemRoute {
        override val route: String = "analysis"
        override val title: String = "Análisis"
        override val bottomTab: AgroGemBottomTab? = null
    }

    data object AnalysisHistory : AgroGemRoute {
        override val route: String = "analysis_history"
        override val title: String = "Ver análisis"
        override val bottomTab: AgroGemBottomTab? = null
    }

    data object Diagnosis : AgroGemRoute {
        override val route: String = "diagnosis"
        override val title: String = "Diagnóstico"
        override val bottomTab: AgroGemBottomTab? = null
    }

    data object TreatmentPlan : AgroGemRoute {
        override val route: String = "treatment_plan"
        override val title: String = "Plan de tratamiento"
        override val bottomTab: AgroGemBottomTab? = null
    }

    data object TreatmentProducts : AgroGemRoute {
        override val route: String = "treatment_products"
        override val title: String = "Insumos sugeridos"
        override val bottomTab: AgroGemBottomTab? = null
    }

    data object ConversationSummary : AgroGemRoute {
        override val route: String = "conversation_summary"
        override val title: String = "Ver conversación"
        override val bottomTab: AgroGemBottomTab? = null
    }

    data object Chat : AgroGemRoute {
        override val route: String = "chat"
        override val title: String = "Chat"
        override val bottomTab: AgroGemBottomTab? = null
    }

    data object ChatConfirm : AgroGemRoute {
        override val route: String = "chat_confirm"
        override val title: String = "Confirmación"
        override val bottomTab: AgroGemBottomTab? = null
    }

    data object History : AgroGemRoute {
        override val route: String = "history"
        override val title: String = "Historial"
        override val bottomTab: AgroGemBottomTab = AgroGemBottomTab.Fields
    }

    data object VoiceReady : AgroGemRoute {
        override val route: String = "voice_ready"
        override val title: String = "Voz"
        override val bottomTab: AgroGemBottomTab = AgroGemBottomTab.Profile
    }

    companion object {
        val all = listOf(
            Home,
            Analysis,
            AnalysisHistory,
            Chat,
            ChatConfirm,
            History,
            VoiceReady,
        )

        fun fromRoute(route: String?): AgroGemRoute =
            all.firstOrNull { it.route == route } ?: Home
    }
}
