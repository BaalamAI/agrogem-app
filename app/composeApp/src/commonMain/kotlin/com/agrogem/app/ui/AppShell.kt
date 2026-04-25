package com.agrogem.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.agrogem.app.data.OnboardingStateStore
import com.agrogem.app.data.rememberImagePickerLauncher
import com.agrogem.app.navigation.AgroGemBottomTab
import com.agrogem.app.navigation.AgroGemRoute
import com.agrogem.app.navigation.AppNavHost
import com.agrogem.app.navigation.navigateTo
import com.agrogem.app.ui.components.BottomNavigationBar
import com.agrogem.app.ui.screens.analysis.AnalysisFlowViewModel
import com.agrogem.app.ui.screens.chat.ChatViewModel
import com.agrogem.app.ui.viewmodel.kmpViewModel

@Composable
fun AppShell(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val onboardingStateStore = remember { OnboardingStateStore() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = AgroGemRoute.fromRoute(backStackEntry?.destination?.route)
    val showBottomBar = currentRoute == AgroGemRoute.Home || currentRoute == AgroGemRoute.History || currentRoute == AgroGemRoute.Conversations
    val currentTab = currentRoute.bottomTab ?: AgroGemBottomTab.Home
    val startDestination = if (onboardingStateStore.isCompleted()) {
        AgroGemRoute.Home.route
    } else {
        AgroGemRoute.Onboarding.createRoute(0)
    }

    // Shared ViewModel for the analysis flow — lives here so it survives navigation
    val analysisFlowVm = kmpViewModel { AnalysisFlowViewModel() }

    // Shared ChatViewModel for the chat/voice flow — lives here so it survives navigation
    // and is shared across Chat, ChatConfirm, and VoiceReady routes (Phase 5 architecture fix).
    // Seeded with null so it starts in Blank mode. When navigating from Analysis → Chat,
    // AppNavHost calls chatViewModel.seedFromAnalysis(...) before pushing the chat route,
    // so the shared instance carries the real analysis context at runtime.
    val chatViewModel = kmpViewModel {
        ChatViewModel(
            analysisId = null,
            diagnosis = null,
        )
    }

    // Camera launcher — opens the native camera directly from the Scan FAB
    val imagePicker = rememberImagePickerLauncher { result ->
        if (result != null) {
            analysisFlowVm.setCapturedImage(result)
            analysisFlowVm.startSimulatedAnalysis()
            navController.navigateTo(AgroGemRoute.Analysis)
        }
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(
                    currentTab = currentTab,
                    onNavigate = { tab ->
                        val destination = when (tab) {
                            AgroGemBottomTab.Home -> AgroGemRoute.Home
                            AgroGemBottomTab.Fields -> AgroGemRoute.History
                            AgroGemBottomTab.Scan -> {
                                // Launch native camera directly — no navigation
                                imagePicker.launchCamera()
                                return@BottomNavigationBar
                            }
                            AgroGemBottomTab.Maps -> AgroGemRoute.TreatmentProducts
                            AgroGemBottomTab.Chat -> AgroGemRoute.Conversations
                        }

                        if (destination.route != backStackEntry?.destination?.route) {
                            navController.navigateTo(destination)
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            analysisFlowVm = analysisFlowVm,
            chatViewModel = chatViewModel,
            startDestination = startDestination,
            onOnboardingFinished = {
                onboardingStateStore.markCompleted()
                navController.navigate(AgroGemRoute.Home.route) {
                    popUpTo(AgroGemRoute.Onboarding.createRoute(0)) {
                        inclusive = true
                    }
                    launchSingleTop = true
                    restoreState = false
                }
            },
            onWelcomeAdvance = {
                navController.navigate(AgroGemRoute.Onboarding.createRoute(1)) {
                    popUpTo(AgroGemRoute.Onboarding.createRoute(0)) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            },
            onWriteWithAgroGemma = {
                navController.navigate(AgroGemRoute.OnboardingChat.route) {
                    launchSingleTop = true
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}
