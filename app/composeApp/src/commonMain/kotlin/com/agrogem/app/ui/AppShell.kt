package com.agrogem.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agrogem.app.data.OnboardingStateStore
import com.agrogem.app.data.auth.createAuthRepository
import com.agrogem.app.data.chat.createChatRepository
import com.agrogem.app.data.climate.createClimateRepository
import com.agrogem.app.data.geolocation.createGeolocationRepository
import com.agrogem.app.data.getGemmaManager
import com.agrogem.app.data.getGemmaModelDownloader
import com.agrogem.app.data.connectivity.createConnectivityMonitor
import com.agrogem.app.data.pest.createPestRepository
import com.agrogem.app.data.pest.domain.PlantAnalysisRepositoryImpl
import com.agrogem.app.data.rememberImagePickerLauncher
import com.agrogem.app.data.rememberSpeechRecognizer
import com.agrogem.app.data.rememberSpeechSynthesizer
import com.agrogem.app.data.risk.createRiskRepository
import com.agrogem.app.data.session.SessionLocalStore
import com.agrogem.app.data.soil.createSoilRepository
import com.agrogem.app.data.weather.createWeatherRepository
import com.agrogem.app.navigation.AgroGemBottomTab
import com.agrogem.app.navigation.AgroGemRoute
import com.agrogem.app.navigation.AppNavHost
import com.agrogem.app.navigation.navigateTo
import com.agrogem.app.ui.components.BottomNavigationBar
import com.agrogem.app.ui.screens.analysis.AnalysisFlowViewModel
import com.agrogem.app.ui.screens.chat.ChatEffect
import com.agrogem.app.ui.screens.chat.ChatViewModel
import com.agrogem.app.ui.screens.chat.ConversationStore
import com.agrogem.app.ui.screens.home.HomeViewModel
import com.agrogem.app.ui.screens.map.MapRiskViewModel
import com.agrogem.app.ui.viewmodel.kmpViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AppShell(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val onboardingStateStore = remember { OnboardingStateStore() }
    val sessionLocalStore = remember { SessionLocalStore() }
    val authRepository = remember { createAuthRepository(sessionLocalStore) }
    val appSessionViewModel = kmpViewModel {
        AppSessionViewModel(authRepository, sessionLocalStore)
    }
    val sessionUiState by appSessionViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        appSessionViewModel.bootstrap()
    }

    LaunchedEffect(sessionUiState.onboardingDone) {
        if (sessionUiState.onboardingDone) {
            onboardingStateStore.markCompleted()
            navController.navigate(AgroGemRoute.Home.route) {
                popUpTo(AgroGemRoute.Onboarding.createRoute(0)) {
                    inclusive = true
                }
                launchSingleTop = true
                restoreState = false
            }
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = AgroGemRoute.fromRoute(backStackEntry?.destination?.route)
    val showBottomBar = currentRoute == AgroGemRoute.Home || currentRoute == AgroGemRoute.History || currentRoute == AgroGemRoute.Conversations || currentRoute == AgroGemRoute.MapRisk
    val currentTab = currentRoute.bottomTab ?: AgroGemBottomTab.Home
    val startDestination = if (onboardingStateStore.isCompleted() || sessionUiState.onboardingDone) {
        AgroGemRoute.Home.route
    } else {
        AgroGemRoute.Onboarding.createRoute(0)
    }

    // Shared repositories for geolocation, weather, soil, and risk — injected into ViewModels
    val geolocationRepository = remember { createGeolocationRepository() }
    val weatherRepository = remember { createWeatherRepository() }
    val soilRepository = remember { createSoilRepository() }
    val climateRepository = remember { createClimateRepository() }
    val riskRepository = remember { createRiskRepository() }
    val homeViewModel = kmpViewModel {
        HomeViewModel(
            geolocationRepository = geolocationRepository,
            weatherRepository = weatherRepository,
            soilRepository = soilRepository,
            sessionLocalStore = sessionLocalStore,
        )
    }
    val mapRiskViewModel = kmpViewModel {
        MapRiskViewModel(
            geolocationRepository = geolocationRepository,
            riskRepository = riskRepository,
        )
    }

    // Shared ViewModel for the analysis flow — lives here so it survives navigation
    val pestRepository = remember { createPestRepository() }
    val connectivityMonitor = remember { createConnectivityMonitor() }
    val plantAnalysisRepository = remember {
        PlantAnalysisRepositoryImpl(
            gemmaManager = getGemmaManager(),
            modelDownloader = getGemmaModelDownloader(),
            pestRepository = pestRepository,
            connectivityMonitor = connectivityMonitor,
        )
    }
    val analysisFlowVm = kmpViewModel { AnalysisFlowViewModel(plantAnalysisRepository = plantAnalysisRepository) }

    // In-memory store for analysis-born conversations — lives while the app process is alive.
    val conversationStore = remember { ConversationStore() }

    // Shared ChatViewModel for the chat/voice flow — lives here so it survives navigation
    // and is shared across Chat, ChatConfirm, and VoiceReady routes (Phase 5 architecture fix).
    // Seeded with null so it starts in Blank mode. When navigating from Analysis → Chat,
    // AppNavHost calls chatViewModel.seedFromAnalysis(...) before pushing the chat route,
    // so the shared instance carries the real analysis context at runtime.
    val chatRepository = remember { createChatRepository(authRepository) }
    val speechRecognizer = rememberSpeechRecognizer()
    val speechSynthesizer = rememberSpeechSynthesizer()
    val chatViewModel = kmpViewModel {
        ChatViewModel(
            chatRepository = chatRepository,
            analysisId = null,
            diagnosis = null,
            gemmaManager = getGemmaManager(),
            gemmaModelDownloader = getGemmaModelDownloader(),
            geolocationRepository = geolocationRepository,
            riskRepository = riskRepository,
            weatherRepository = weatherRepository,
            soilRepository = soilRepository,
            connectivityMonitor = connectivityMonitor,
            sessionLocalStore = sessionLocalStore,
            speechRecognizer = speechRecognizer,
            speechSynthesizer = speechSynthesizer,
        )
    }

    LaunchedEffect(Unit) {
        chatViewModel.effects.collectLatest { effect ->
            when (effect) {
                is ChatEffect.SessionExpired -> appSessionViewModel.reportSessionExpired()
            }
        }
    }

    // Camera launcher — opens the native camera directly from the Scan FAB
    val imagePicker = rememberImagePickerLauncher { result ->
        if (result != null) {
            analysisFlowVm.startAnalysis(result)
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
                            AgroGemBottomTab.Maps -> AgroGemRoute.MapRisk
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
            conversationStore = conversationStore,
            appSessionViewModel = appSessionViewModel,
            homeViewModel = homeViewModel,
            mapRiskViewModel = mapRiskViewModel,
            soilRepository = soilRepository,
            climateRepository = climateRepository,
            geolocationRepository = geolocationRepository,
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
