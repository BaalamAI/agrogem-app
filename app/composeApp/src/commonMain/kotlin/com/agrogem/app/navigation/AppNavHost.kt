package com.agrogem.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.agrogem.app.data.rememberImagePickerLauncher
import com.agrogem.app.ui.screens.analysis.AnalysisFlowViewModel
import com.agrogem.app.ui.screens.analysis.PlantAnalysisScreen
import com.agrogem.app.ui.screens.chat.ChatEvent
import com.agrogem.app.ui.screens.chat.ChatScreen
import com.agrogem.app.ui.screens.chat.ChatViewModel
import com.agrogem.app.ui.screens.chat.ConversationsScreen
import com.agrogem.app.ui.screens.chat.ConversationsViewModel
import com.agrogem.app.ui.screens.chat.VoiceReadyScreen
import com.agrogem.app.data.climate.domain.ClimateRepository
import com.agrogem.app.data.climate.domain.createDefaultClimateQuery
import com.agrogem.app.data.geolocation.domain.GeolocationRepository
import com.agrogem.app.data.soil.domain.SoilRepository
import com.agrogem.app.ui.AppSessionViewModel
import com.agrogem.app.ui.screens.onboarding.OnboardingChatScreen
import com.agrogem.app.ui.screens.onboarding.OnboardingChatViewModel
import com.agrogem.app.ui.screens.onboarding.OnboardingScreen
import com.agrogem.app.theme.AgroGemColors
import com.agrogem.app.ui.screens.history.HistoryScreen
import com.agrogem.app.ui.screens.home.HomeScreen
import com.agrogem.app.ui.screens.home.HomeViewModel
import com.agrogem.app.ui.screens.environment.EnvironmentDetailScreen
import com.agrogem.app.ui.screens.environment.EnvironmentDetailViewModel
import com.agrogem.app.ui.screens.map.MapRiskScreen
import com.agrogem.app.ui.screens.map.MapRiskViewModel
import com.agrogem.app.ui.viewmodel.kmpViewModel

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    analysisFlowVm: AnalysisFlowViewModel,
    chatViewModel: ChatViewModel,
    appSessionViewModel: AppSessionViewModel,
    homeViewModel: HomeViewModel,
    mapRiskViewModel: MapRiskViewModel,
    soilRepository: SoilRepository,
    climateRepository: ClimateRepository,
    geolocationRepository: GeolocationRepository,
    startDestination: String = AgroGemRoute.Home.route,
    onOnboardingFinished: () -> Unit = {},
    onWelcomeAdvance: () -> Unit = {},
    onWriteWithAgroGemma: () -> Unit = {},
) {
    val chatImagePicker = rememberImagePickerLauncher { result ->
        if (result != null) {
            chatViewModel.onEvent(ChatEvent.ImageSelected(result.uri))
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(AgroGemRoute.Home.route) {
            HomeScreen(
                viewModel = homeViewModel,
                onOpenCamera = { /* Camera is launched via FAB in AppShell */ },
                onOpenHistory = { navController.pushTo(AgroGemRoute.History) },
                onOpenEnvironmentDetail = { navController.pushTo(AgroGemRoute.Environment) },
            )
        }

        composable(
            route = AgroGemRoute.Onboarding.NAV_ROUTE,
            arguments = listOf(
                navArgument(AgroGemRoute.Onboarding.STEP_ARG) {
                    type = NavType.IntType
                    defaultValue = 0
                },
            ),
        ) { backStackEntry ->
            val step = backStackEntry.savedStateHandle.get<Int>(AgroGemRoute.Onboarding.STEP_ARG) ?: 0
            OnboardingScreen(
                step = step,
                onFinish = onOnboardingFinished,
                onWelcomeAdvance = onWelcomeAdvance,
                onWriteWithAgroGemma = onWriteWithAgroGemma,
            )
        }

        composable(AgroGemRoute.OnboardingChat.route) {
            val onboardingChatViewModel = kmpViewModel { OnboardingChatViewModel() }
            OnboardingChatScreen(
                viewModel = onboardingChatViewModel,
                appSessionViewModel = appSessionViewModel,
                onBack = { navController.popBackStack() },
            )
        }

        composable(AgroGemRoute.Camera.route) {
            PlaceholderRouteScreen(
                title = "Camara",
                subtitle = "Usa el boton Scan para abrir la camara nativa.",
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

        composable(AgroGemRoute.Conversations.route) {
            val conversationsViewModel = kmpViewModel { ConversationsViewModel() }
            ConversationsScreen(
                viewModel = conversationsViewModel,
                onOpenConversation = { conversation ->
                    if (conversation.analysisId != null && conversation.diagnosis != null) {
                        chatViewModel.seedFromAnalysis(conversation.analysisId, conversation.diagnosis)
                        navController.pushTo(AgroGemRoute.Chat.createRoute(conversation.analysisId))
                    } else {
                        chatViewModel.resetToBlank()
                        navController.pushTo(AgroGemRoute.Chat.createRoute(null))
                    }
                },
                onNewChat = {
                    chatViewModel.resetToBlank()
                    navController.pushTo(AgroGemRoute.Chat.createRoute(null))
                },
            )
        }

        composable(AgroGemRoute.Diagnosis.route) {
            PlaceholderRouteScreen(
                title = "Diagnostico",
                subtitle = "Pantalla en preparacion.",
            )
        }

        composable(AgroGemRoute.TreatmentPlan.route) {
            PlaceholderRouteScreen(
                title = "Plan de tratamiento",
                subtitle = "Pantalla en preparacion.",
            )
        }

        composable(AgroGemRoute.TreatmentProducts.route) {
            PlaceholderRouteScreen(
                title = "Insumos sugeridos",
                subtitle = "Pantalla en preparacion.",
            )
        }

        composable(AgroGemRoute.MapRisk.route) {
            MapRiskScreen(viewModel = mapRiskViewModel)
        }

        composable(AgroGemRoute.ConversationSummary.route) {
            PlaceholderRouteScreen(
                title = "Resumen de conversacion",
                subtitle = "Pantalla en preparacion.",
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
                onTalkToAgent = { analysisId, diagnosis ->
                    // Post-analysis handoff: seed the shared ChatViewModel with the
                    // analysis context, then navigate to the chat screen.
                    chatViewModel.seedFromAnalysis(analysisId, diagnosis)
                    navController.pushTo(AgroGemRoute.Chat.createRoute(analysisId))
                },
            )
        }

        // Analysis opened from history — exit button says "Regresar"
        composable(AgroGemRoute.AnalysisHistory.route) {
            PlantAnalysisScreen(
                viewModel = analysisFlowVm,
                fromHistory = true,
                onCancel = { navController.popBackStack() },
                onExit = { navController.popBackStack() },
                onTalkToAgent = { analysisId, diagnosis ->
                    // Post-analysis handoff: seed the shared ChatViewModel with the
                    // analysis context, then navigate to the chat screen.
                    chatViewModel.seedFromAnalysis(analysisId, diagnosis)
                    navController.pushTo(AgroGemRoute.Chat.createRoute(analysisId))
                },
            )
        }

        composable(AgroGemRoute.VoiceReady.route) {
            // Use shared chatViewModel from AppShell — same instance as Chat and ChatConfirm
            VoiceReadyScreen(
                voiceState = chatViewModel.uiState.value.voiceState,
                onDismiss = {
                    chatViewModel.onEvent(ChatEvent.DismissVoice)
                    navController.popBackStack()
                },
                onStopRecording = {
                    chatViewModel.onEvent(ChatEvent.StopVoiceInput)
                    navController.popBackStack()
                },
            )
        }

        composable(
            route = AgroGemRoute.Chat.NAV_ROUTE,
            arguments = listOf(
                navArgument(AgroGemRoute.Chat.ANALYSIS_ID_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            // Use shared chatViewModel from AppShell — same instance as VoiceReady
            ChatScreen(
                viewModel = chatViewModel,
                onBack = { navController.popBackStack() },
                onRequestClose = { navController.pushTo(AgroGemRoute.ChatConfirm) },
                onMicClick = {
                    chatViewModel.onEvent(ChatEvent.StartVoiceInput)
                    navController.pushTo(AgroGemRoute.VoiceReady)
                },
                onLaunchCamera = { chatImagePicker.launchCamera() },
                onLaunchGallery = { chatImagePicker.launchGallery() },
                showConfirmDialog = false,
            )
        }

        composable(AgroGemRoute.ChatConfirm.route) {
            // Use shared chatViewModel from AppShell — same instance as Chat and VoiceReady
            ChatScreen(
                viewModel = chatViewModel,
                onBack = { navController.popBackStack() },
                onRequestClose = {},
                onMicClick = {
                    chatViewModel.onEvent(ChatEvent.StartVoiceInput)
                    navController.pushTo(AgroGemRoute.VoiceReady)
                },
                onLaunchCamera = { chatImagePicker.launchCamera() },
                onLaunchGallery = { chatImagePicker.launchGallery() },
                showConfirmDialog = true,
                onConfirmClose = {
                    navController.popBackStack(AgroGemRoute.Chat.NAV_ROUTE, inclusive = true)
                },
            )
        }

        composable(AgroGemRoute.Environment.route) {
            val location by geolocationRepository.observeResolvedLocation()
                .collectAsStateWithLifecycle(initialValue = null)
            val resolvedLocation = location

            if (resolvedLocation != null) {
                val query = createDefaultClimateQuery()
                val envVm = kmpViewModel {
                    EnvironmentDetailViewModel(
                        soilRepository = soilRepository,
                        climateRepository = climateRepository,
                        location = resolvedLocation,
                        query = query,
                    )
                }
                EnvironmentDetailScreen(
                    viewModel = envVm,
                    onBack = { navController.popBackStack() },
                )
            } else {
                PlaceholderRouteScreen(
                    title = "Perfil ambiental",
                    subtitle = "Seleccioná una ubicación para ver el perfil.",
                )
            }
        }
    }
}

@Composable
private fun PlaceholderRouteScreen(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AgroGemColors.Screen),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$title\n$subtitle",
            color = Color.Black,
        )
    }
}

private fun NavHostController.pushTo(route: AgroGemRoute) {
    navigate(route.route) {
        launchSingleTop = true
    }
}

private fun NavHostController.pushTo(routeString: String) {
    navigate(routeString) {
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
