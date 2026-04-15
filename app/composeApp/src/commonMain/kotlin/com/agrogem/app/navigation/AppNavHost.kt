package com.agrogem.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.agrogem.app.ui.screens.figma.AnalysisProgressFigmaScreen
import com.agrogem.app.ui.screens.figma.CameraCaptureFigmaScreen
import com.agrogem.app.ui.screens.figma.ChatConversationFigmaScreen
import com.agrogem.app.ui.screens.figma.ConversationSummaryFigmaScreen
import com.agrogem.app.ui.screens.figma.DiagnosisFigmaScreen
import com.agrogem.app.ui.screens.figma.HistoryFigmaScreen
import com.agrogem.app.ui.screens.figma.HomeFigmaScreen
import com.agrogem.app.ui.screens.figma.TreatmentPlanFigmaScreen
import com.agrogem.app.ui.screens.figma.TreatmentProductsFigmaScreen
import com.agrogem.app.ui.screens.figma.VoiceReadyFigmaScreen

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: AgroGemRoute = AgroGemRoute.Home,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination.route,
        modifier = modifier,
    ) {
        composable(AgroGemRoute.Home.route) {
            HomeFigmaScreen(
                onOpenCamera = { navController.pushTo(AgroGemRoute.Camera) },
                onOpenHistory = { navController.pushTo(AgroGemRoute.History) },
            )
        }

        composable(AgroGemRoute.History.route) {
            HistoryFigmaScreen(
                onOpenEntry = { navController.pushTo(AgroGemRoute.ConversationSummary) },
            )
        }

        composable(AgroGemRoute.Camera.route) {
            CameraCaptureFigmaScreen(
                onClose = { navController.navigateTo(AgroGemRoute.Home) },
                onAnalyze = { navController.pushTo(AgroGemRoute.Analysis) },
            )
        }

        composable(AgroGemRoute.Analysis.route) {
            AnalysisProgressFigmaScreen(
                onCancel = { navController.navigateTo(AgroGemRoute.Camera) },
                onContinue = { navController.pushTo(AgroGemRoute.Diagnosis) },
            )
        }

        composable(AgroGemRoute.Diagnosis.route) {
            DiagnosisFigmaScreen(
                onOpenPlan = { navController.pushTo(AgroGemRoute.TreatmentPlan) },
            )
        }

        composable(AgroGemRoute.TreatmentPlan.route) {
            TreatmentPlanFigmaScreen(
                onSaveAndExit = { navController.navigateTo(AgroGemRoute.Home) },
                onTalk = { navController.pushTo(AgroGemRoute.Chat) },
                onOpenProducts = { navController.pushTo(AgroGemRoute.TreatmentProducts) },
            )
        }

        composable(AgroGemRoute.TreatmentProducts.route) {
            TreatmentProductsFigmaScreen(
                onSaveAndExit = { navController.navigateTo(AgroGemRoute.Home) },
                onTalk = { navController.pushTo(AgroGemRoute.Chat) },
                onOpenConversationSummary = { navController.pushTo(AgroGemRoute.ConversationSummary) },
            )
        }

        composable(AgroGemRoute.ConversationSummary.route) {
            ConversationSummaryFigmaScreen(
                onViewConversation = { navController.pushTo(AgroGemRoute.Chat) },
            )
        }

        composable(AgroGemRoute.Chat.route) {
            ChatConversationFigmaScreen(
                onBack = { navController.popBackStack() },
                onRequestClose = { navController.pushTo(AgroGemRoute.ChatConfirm) },
                showConfirmDialog = false,
            )
        }

        composable(AgroGemRoute.ChatConfirm.route) {
            ChatConversationFigmaScreen(
                onBack = { navController.popBackStack() },
                onRequestClose = {},
                showConfirmDialog = true,
                onConfirmClose = {
                    navController.popBackStack(AgroGemRoute.Chat.route, inclusive = true)
                },
            )
        }

        composable(AgroGemRoute.VoiceReady.route) {
            VoiceReadyFigmaScreen(
                onBack = { navController.navigateTo(AgroGemRoute.Home) },
                onOpenChat = { navController.pushTo(AgroGemRoute.Chat) },
            )
        }
    }
}

private fun NavHostController.pushTo(route: AgroGemRoute) {
    navigate(route.route) {
        launchSingleTop = true
    }
}

fun NavHostController.navigateTo(route: AgroGemRoute) {
    navigate(route.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
