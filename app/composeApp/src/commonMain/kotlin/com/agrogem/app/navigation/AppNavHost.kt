package com.agrogem.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.agrogem.app.ui.screens.analysis.AnalysisScreen
import com.agrogem.app.ui.screens.camera.CameraScreen
import com.agrogem.app.ui.screens.dashboard.DashboardScreen
import com.agrogem.app.ui.screens.map.MapRiskScreen
import com.agrogem.app.ui.screens.report.ReportScreen

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: AgroGemRoute = AgroGemRoute.Dashboard,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination.route,
        modifier = modifier,
    ) {
        composable(AgroGemRoute.Dashboard.route) {
            DashboardScreen()
        }
        composable(AgroGemRoute.Camera.route) {
            CameraScreen(
                onStartAnalysis = { navController.navigateTo(AgroGemRoute.Analysis) },
            )
        }
        composable(AgroGemRoute.Map.route) {
            MapRiskScreen(
                onBackToDashboard = { navController.navigateTo(AgroGemRoute.Dashboard) },
            )
        }
        composable(AgroGemRoute.Analysis.route) {
            AnalysisScreen(
                onBackToCamera = { navController.navigateTo(AgroGemRoute.Camera) },
                onViewReport = { navController.navigateTo(AgroGemRoute.Report) },
            )
        }
        composable(AgroGemRoute.Report.route) {
            ReportScreen(
                onScanAgain = { navController.navigateTo(AgroGemRoute.Camera) },
                onBackToDashboard = { navController.navigateTo(AgroGemRoute.Dashboard) },
            )
        }
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
