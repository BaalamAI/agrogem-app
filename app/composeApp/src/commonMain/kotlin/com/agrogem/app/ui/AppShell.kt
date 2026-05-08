package com.agrogem.app.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.agrogem.app.data.GemmaPreparation
import com.agrogem.app.data.OnboardingStateStore
import com.agrogem.app.data.analysis.createAnalysisRepository
import com.agrogem.app.data.auth.createAuthRepository
import com.agrogem.app.data.chat.createChatRepository
import com.agrogem.app.data.chat.createLocalChatRepository
import com.agrogem.app.data.climate.createClimateRepository
import com.agrogem.app.data.connectivity.createConnectivityMonitor
import com.agrogem.app.data.createGemmaManager
import com.agrogem.app.data.createGemmaModelDownloader
import com.agrogem.app.data.environment.createEnvironmentRepository
import com.agrogem.app.data.geolocation.createGeolocationRepository
import com.agrogem.app.data.pest.createPestRepository
import com.agrogem.app.data.pest.domain.PlantAnalysisRepositoryImpl
import com.agrogem.app.data.location.BindLocationGate
import com.agrogem.app.data.rememberAudioRecorder
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
import com.agrogem.app.ui.screens.home.HomeViewModel
import com.agrogem.app.ui.screens.map.MapRiskViewModel
import androidx.compose.ui.Modifier
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

    LaunchedEffect(Unit) {
        appSessionViewModel.bootstrap()
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = AgroGemRoute.fromRoute(backStackEntry?.destination?.route)
    // POC reducido: solo Home + Chat. La barra solo aparece en Home (Chat la oculta como antes).
    val showBottomBar = currentRoute == AgroGemRoute.Home
    val currentTab = currentRoute.bottomTab ?: AgroGemBottomTab.Home
    // POC: onboarding is intentionally bypassed — always launch into Home.
    val startDestination = AgroGemRoute.Home.route

    val geolocationRepository = remember { createGeolocationRepository() }
    val weatherRepository = remember { createWeatherRepository() }
    val soilRepository = remember { createSoilRepository() }
    val climateRepository = remember { createClimateRepository() }
    val riskRepository = remember { createRiskRepository() }
    val analysisRepository = remember { createAnalysisRepository() }
    val homeViewModel = kmpViewModel {
        HomeViewModel(
            analysisRepository = analysisRepository,
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

    val pestRepository = remember { createPestRepository() }
    val connectivityMonitor = remember { createConnectivityMonitor() }
    val gemmaManager = remember { createGemmaManager() }
    val gemmaDownloader = remember { createGemmaModelDownloader() }
    val gemmaModelPreference = remember { com.agrogem.app.data.GemmaModelPreferenceStore() }
    val gemmaPreparation = remember {
        GemmaPreparation(
            gemmaManager = gemmaManager,
            modelDownloader = gemmaDownloader,
            modelPreference = gemmaModelPreference,
        )
    }
    val plantAnalysisRepository = remember {
        PlantAnalysisRepositoryImpl(
            gemmaManager = gemmaManager,
            gemmaPreparation = gemmaPreparation,
            pestRepository = pestRepository,
            connectivityMonitor = connectivityMonitor,
        )
    }
    val analysisFlowVm = kmpViewModel {
        AnalysisFlowViewModel(
            plantAnalysisRepository = plantAnalysisRepository,
            analysisRepository = analysisRepository,
        )
    }

    val localChatRepository = remember { createLocalChatRepository() }

    val chatRepository = remember { createChatRepository(authRepository) }
    val environmentRepository = remember { createEnvironmentRepository() }
    val speechRecognizer = rememberSpeechRecognizer()
    val speechSynthesizer = rememberSpeechSynthesizer()
    val audioRecorder = rememberAudioRecorder { }
    val chatViewModel = kmpViewModel {
        ChatViewModel(
            chatRepository = chatRepository,
            localChatRepository = localChatRepository,
            analysisId = null,
            diagnosis = null,
            gemmaManager = gemmaManager,
            gemmaModelDownloader = gemmaDownloader,
            gemmaPreparation = gemmaPreparation,
            geolocationRepository = geolocationRepository,
            riskRepository = riskRepository,
            environmentRepository = environmentRepository,
            connectivityMonitor = connectivityMonitor,
            sessionLocalStore = sessionLocalStore,
            speechRecognizer = speechRecognizer,
            speechSynthesizer = speechSynthesizer,
            audioRecorder = audioRecorder,
        )
    }

    LaunchedEffect(Unit) {
        chatViewModel.effects.collectLatest { effect ->
            when (effect) {
                is ChatEffect.SessionExpired -> appSessionViewModel.reportSessionExpired()
            }
        }
    }

    BindLocationGate()

    LaunchedEffect(Unit) {
        gemmaPreparation.ensureReady()
    }

    val imagePicker = rememberImagePickerLauncher { result ->
        if (result != null) {
            analysisFlowVm.startAnalysis(result)
            navController.navigateTo(AgroGemRoute.Analysis)
        }
    }

    val contentInsets = if (currentRoute == AgroGemRoute.Chat) {
        WindowInsets.statusBars
    } else {
        ScaffoldDefaults.contentWindowInsets
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = contentInsets,
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(
                    currentTab = currentTab,
                    onNavigate = { tab ->
                        val destination = when (tab) {
                            AgroGemBottomTab.Home -> AgroGemRoute.Home
                            AgroGemBottomTab.Fields -> AgroGemRoute.History
                            AgroGemBottomTab.Scan -> {
                                imagePicker.launchCamera()
                                return@BottomNavigationBar
                            }
                            AgroGemBottomTab.Maps -> AgroGemRoute.MapRisk
                            AgroGemBottomTab.Chat -> {
                                chatViewModel.resetToBlank()
                                navController.navigate(AgroGemRoute.Chat.createRoute(null)) {
                                    launchSingleTop = true
                                }
                                return@BottomNavigationBar
                            }
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
            analysisRepository = analysisRepository,
            chatViewModel = chatViewModel,
            localChatRepository = localChatRepository,
            appSessionViewModel = appSessionViewModel,
            homeViewModel = homeViewModel,
            mapRiskViewModel = mapRiskViewModel,
            soilRepository = soilRepository,
            climateRepository = climateRepository,
            geolocationRepository = geolocationRepository,
            gemmaPreparation = gemmaPreparation,
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
