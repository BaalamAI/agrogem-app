package com.agrogem.app.navigation

enum class AgroGemBottomTab {
    Home,
    Fields,
    Scan,
    Maps,
    Chat,
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

    data object Onboarding : AgroGemRoute {
        override val route: String = "onboarding"
        override val title: String = "Onboarding"
        override val bottomTab: AgroGemBottomTab? = null

        const val BASE_ROUTE = "onboarding"
        const val STEP_ARG = "step"
        const val NAV_ROUTE = "$BASE_ROUTE?$STEP_ARG={$STEP_ARG}"

        fun createRoute(step: Int = 0): String = "$BASE_ROUTE?$STEP_ARG=$step"
    }

    data object OnboardingChat : AgroGemRoute {
        override val route: String = "onboarding_chat"
        override val title: String = "Onboarding chat"
        override val bottomTab: AgroGemBottomTab? = null
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
        override val route: String = BASE_ROUTE
        override val title: String = "Chat"
        override val bottomTab: AgroGemBottomTab? = null

        /** Base route without query params — used for back-stack matching. */
        const val BASE_ROUTE = "chat"
        const val ANALYSIS_ID_ARG = "analysisId"
        const val NAV_ROUTE = "$BASE_ROUTE?$ANALYSIS_ID_ARG={$ANALYSIS_ID_ARG}"

        fun createRoute(analysisId: String? = null): String {
            return if (!analysisId.isNullOrBlank()) {
                "$BASE_ROUTE?$ANALYSIS_ID_ARG=$analysisId"
            } else {
                BASE_ROUTE
            }
        }
    }

    data object ChatConfirm : AgroGemRoute {
        override val route: String = "chat_confirm"
        override val title: String = "Confirmación"
        override val bottomTab: AgroGemBottomTab? = null
    }

    data object GemmaDemo : AgroGemRoute {
        override val route: String = "gemma_demo"
        override val title: String = "Gemma 4 Demo"
        override val bottomTab: AgroGemBottomTab? = null
    }

    data object History : AgroGemRoute {
        override val route: String = "history"
        override val title: String = "Historial"
        override val bottomTab: AgroGemBottomTab = AgroGemBottomTab.Fields
    }

    data object Conversations : AgroGemRoute {
        override val route: String = "conversations"
        override val title: String = "Conversaciones"
        override val bottomTab: AgroGemBottomTab = AgroGemBottomTab.Chat
    }

    data object VoiceReady : AgroGemRoute {
        override val route: String = "voice_ready"
        override val title: String = "Voz"
        override val bottomTab: AgroGemBottomTab? = null
    }

    data object MapRisk : AgroGemRoute {
        override val route: String = "map_risk"
        override val title: String = "Mapa de riesgo"
        override val bottomTab: AgroGemBottomTab = AgroGemBottomTab.Maps
    }

    data object Environment : AgroGemRoute {
        override val route: String = "environment"
        override val title: String = "Perfil ambiental"
        override val bottomTab: AgroGemBottomTab? = null
    }

    companion object {
        val all = listOf(
            Home,
            Onboarding,
            OnboardingChat,
            Camera,
            Analysis,
            AnalysisHistory,
            Diagnosis,
            TreatmentPlan,
            TreatmentProducts,
            ConversationSummary,
            Chat,
            ChatConfirm,
            GemmaDemo,
            History,
            Conversations,
            VoiceReady,
            MapRisk,
            Environment,
        )

        fun fromRoute(route: String?): AgroGemRoute {
            if (route == null) return Home
            // Strip query params to match route patterns like "chat?analysisId=123"
            val base = route.substringBefore("?")
            return all.firstOrNull { it.route.substringBefore("?") == base } ?: Home
        }
    }
}
