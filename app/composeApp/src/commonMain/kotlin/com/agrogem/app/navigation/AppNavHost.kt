package com.agrogem.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.agrogem.app.ui.screens.analysis.AnalysisFlowViewModel
import com.agrogem.app.ui.screens.analysis.PlantAnalysisScreen
import com.agrogem.app.ui.screens.chat.ChatScreen
import com.agrogem.app.ui.screens.history.HistoryScreen
import com.agrogem.app.ui.screens.home.HomeScreen

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    analysisFlowVm: AnalysisFlowViewModel,
    startDestination: AgroGemRoute = AgroGemRoute.Home,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination.route,
        modifier = modifier,
    ) {
        composable(AgroGemRoute.Home.route) {
            HomeScreen(
                onOpenCamera = { /* Camera is launched via FAB in AppShell */ },
                onOpenHistory = { navController.pushTo(AgroGemRoute.History) },
            )
        }

        composable(AgroGemRoute.History.route) {
            HistoryScreen(
                onOpenEntry = {
                    analysisFlowVm.loadFromHistory(imageUri = "")
                    navController.pushTo(AgroGemRoute.AnalysisHistory)
                },
            )
        }

        // Analysis opened from camera — exit button says "Guardar y salir"
        composable(AgroGemRoute.Analysis.route) {
            PlantAnalysisScreen(
                viewModel = analysisFlowVm,
                fromHistory = false,
                onCancel = {
                    analysisFlowVm.cancelAnalysis()
                    navController.navigateTo(AgroGemRoute.Home)
                },
                onExit = {
                    analysisFlowVm.clearAll()
                    navController.navigateTo(AgroGemRoute.Home)
                },
                onTalkToAgent = { navController.pushTo(AgroGemRoute.Chat) },
            )
        }

        // Analysis opened from history — exit button says "Regresar"
        composable(AgroGemRoute.AnalysisHistory.route) {
            PlantAnalysisScreen(
                viewModel = analysisFlowVm,
                fromHistory = true,
                onCancel = { navController.popBackStack() },
                onExit = { navController.popBackStack() },
                onTalkToAgent = { navController.pushTo(AgroGemRoute.Chat) },
            )
        }

        composable(AgroGemRoute.Chat.route) {
            ChatScreen(
                onBack = { navController.popBackStack() },
                onRequestClose = { navController.pushTo(AgroGemRoute.ChatConfirm) },
                showConfirmDialog = false,
            )
        }

        composable(AgroGemRoute.ChatConfirm.route) {
            ChatScreen(
                onBack = { navController.popBackStack() },
                onRequestClose = {},
                showConfirmDialog = true,
                onConfirmClose = {
                    navController.popBackStack(AgroGemRoute.Chat.route, inclusive = true)
                },
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
