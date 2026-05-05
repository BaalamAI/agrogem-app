package com.agrogem.app.ui.screens.onboarding

import com.agrogem.app.data.GemmaPreparationStatus
import com.agrogem.app.data.geolocation.domain.GeocodeResolved
import com.agrogem.app.data.geolocation.domain.GeolocationRepository
import com.agrogem.app.data.geolocation.domain.LocationDisplay
import com.agrogem.app.data.geolocation.domain.ResolvedLocation
import com.agrogem.app.data.location.DeviceLocationProvider
import com.agrogem.app.data.shared.domain.LatLng
import com.agrogem.app.ui.screens.chat.MessageSender
import com.agrogem.app.ui.screens.onboarding.OnboardingChatStage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OnboardingChatViewModelTest {

    private fun readyViewModel(): OnboardingChatViewModel =
        OnboardingChatViewModel(
            assistant = FakeOnboardingAssistant(GemmaPreparationStatus.Ready),
            geolocationRepository = FakeGeolocationRepository(),
            deviceLocationProvider = FakeDeviceLocationProvider(Result.failure(Exception("unused"))),
        )

    @Test
    fun `startOnboardingChat exposes preparing status when assistant is preparing`() {
        val assistant = FakeOnboardingAssistant(GemmaPreparationStatus.Preparing)
        val viewModel = OnboardingChatViewModel(
            assistant = assistant,
            geolocationRepository = FakeGeolocationRepository(),
            deviceLocationProvider = FakeDeviceLocationProvider(Result.failure(Exception("unused"))),
        )

        viewModel.startOnboardingChat()

        assertEquals(GemmaPreparationStatus.Preparing, viewModel.uiState.value.gemmaPreparationStatus)
        assertEquals(OnboardingChatStage.Preparing, viewModel.uiState.value.onboardingChatStage)
    }

    @Test
    fun `ui state reflects unavailable status for fallback mode messaging`() {
        val assistant = FakeOnboardingAssistant(GemmaPreparationStatus.Unavailable("model missing"))
        val viewModel = OnboardingChatViewModel(
            assistant = assistant,
            geolocationRepository = FakeGeolocationRepository(),
            deviceLocationProvider = FakeDeviceLocationProvider(Result.failure(Exception("unused"))),
        )

        viewModel.startOnboardingChat()

        assertTrue(viewModel.uiState.value.gemmaPreparationStatus is GemmaPreparationStatus.Unavailable)
        assertEquals(OnboardingChatStage.Conversation, viewModel.uiState.value.onboardingChatStage)
        assertTrue(viewModel.uiState.value.messages.first().text.contains("guiando"))
    }

    // ========== Initialization Tests ==========

    @Test
    fun `default initialization starts with empty messages and null stage`() {
        val viewModel = readyViewModel()

        val state = viewModel.uiState.value
        assertEquals(emptyList(), state.messages)
        assertEquals("", state.inputText)
        assertEquals(null, state.onboardingChatStage)
        assertEquals(0, state.onboardingStep)
        assertEquals(false, state.alertsEnabled)
    }

    // ========== Onboarding Demo Step-Based Tests ==========

    @Test
    fun `startOnboardingChat sets only initial assistant message and step 0`() {
        val viewModel = readyViewModel()
        viewModel.startOnboardingChat()

        val state = viewModel.uiState.value
        assertEquals(1, state.messages.size)
        assertEquals(MessageSender.Assistant, state.messages[0].sender)
        assertTrue(state.messages[0].text.contains("llamás"))
        assertEquals(OnboardingChatStage.Conversation, state.onboardingChatStage)
        assertEquals(0, state.onboardingStep)
    }

    @Test
    fun `startOnboardingChat is idempotent and does not reset existing onboarding state`() {
        val viewModel = readyViewModel()
        viewModel.startOnboardingChat()
        viewModel.sendOnboardingMessage("Me llamo Juan")

        val before = viewModel.uiState.value
        assertEquals(3, before.messages.size)
        assertEquals(1, before.onboardingStep)

        viewModel.startOnboardingChat()

        val after = viewModel.uiState.value
        assertEquals(3, after.messages.size)
        assertEquals(1, after.onboardingStep)
    }

    @Test
    fun `sendOnboardingMessage step 0 asks for crops and stays in Conversation`() {
        val viewModel = readyViewModel()
        viewModel.startOnboardingChat()

        viewModel.sendOnboardingMessage("Me llamo Juan")

        val state = viewModel.uiState.value
        assertEquals(3, state.messages.size)
        assertEquals(MessageSender.User, state.messages[1].sender)
        assertEquals("Me llamo Juan", state.messages[1].text)
        assertEquals(MessageSender.Assistant, state.messages[2].sender)
        assertTrue(state.messages[2].text.contains("cultivo"))
        assertEquals(OnboardingChatStage.Conversation, state.onboardingChatStage)
        assertEquals(1, state.onboardingStep)
    }

    @Test
    fun `sendOnboardingMessage step 1 asks for size and stays in Conversation`() {
        val viewModel = readyViewModel()
        viewModel.startOnboardingChat()

        viewModel.sendOnboardingMessage("Me llamo Juan")
        viewModel.sendOnboardingMessage("Tengo tomate y aguacate")

        val state = viewModel.uiState.value
        assertEquals(5, state.messages.size)
        assertEquals(MessageSender.Assistant, state.messages[4].sender)
        assertTrue(state.messages[4].text.contains("hectáreas") || state.messages[4].text.contains("dimensiones"))
        assertEquals(OnboardingChatStage.Conversation, state.onboardingChatStage)
        assertEquals(2, state.onboardingStep)
    }

    @Test
    fun `sendOnboardingMessage step 2 asks for stage and stays in Conversation`() {
        val viewModel = readyViewModel()
        viewModel.startOnboardingChat()

        viewModel.sendOnboardingMessage("Me llamo Juan")
        viewModel.sendOnboardingMessage("Tengo tomate")
        viewModel.sendOnboardingMessage("100m2")

        val state = viewModel.uiState.value
        assertEquals(7, state.messages.size)
        assertEquals(MessageSender.Assistant, state.messages[6].sender)
        assertTrue(state.messages[6].text.contains("etapa") || state.messages[6].text.contains("Alistando"))
        assertEquals(OnboardingChatStage.Conversation, state.onboardingChatStage)
        assertEquals(3, state.onboardingStep)
    }

    @Test
    fun `sendOnboardingMessage step 3 asks for location and transitions to AwaitingLocationPermission`() {
        val viewModel = readyViewModel()
        viewModel.startOnboardingChat()

        viewModel.sendOnboardingMessage("Me llamo Juan")
        viewModel.sendOnboardingMessage("Tengo tomate")
        viewModel.sendOnboardingMessage("100m2")
        viewModel.sendOnboardingMessage("Está en crecimiento")

        val state = viewModel.uiState.value
        assertEquals(9, state.messages.size)
        assertEquals(MessageSender.Assistant, state.messages[8].sender)
        assertTrue(state.messages[8].text.contains("ubicación") || state.messages[8].text.contains("zona"))
        assertEquals(OnboardingChatStage.AwaitingLocationPermission, state.onboardingChatStage)
        assertEquals(4, state.onboardingStep)
    }

    @Test
    fun `sendOnboardingMessage does nothing when chat has not started`() {
        val viewModel = readyViewModel()
        // Default stage is null, chat not started

        viewModel.sendOnboardingMessage("Hola")

        assertEquals(emptyList(), viewModel.uiState.value.messages)
    }

    @Test
    fun `sendOnboardingMessage does nothing when not in Conversation stage`() {
        val viewModel = readyViewModel()
        viewModel.startOnboardingChat()
        viewModel.sendOnboardingMessage("Me llamo Juan")
        viewModel.sendOnboardingMessage("Tengo tomate")
        viewModel.sendOnboardingMessage("100m2")
        viewModel.sendOnboardingMessage("Está en crecimiento")

        // Now in AwaitingLocationPermission
        assertEquals(OnboardingChatStage.AwaitingLocationPermission, viewModel.uiState.value.onboardingChatStage)

        viewModel.sendOnboardingMessage("Mensaje extra")

        // Should not add more messages
        assertEquals(9, viewModel.uiState.value.messages.size)
    }

    @Test
    fun `sendOnboardingMessage ignores empty input`() {
        val viewModel = readyViewModel()
        viewModel.startOnboardingChat()

        viewModel.sendOnboardingMessage("   ")

        assertEquals(1, viewModel.uiState.value.messages.size)
    }

    @Test
    fun `sendOnboardingMessage clears inputText`() {
        val viewModel = readyViewModel()
        viewModel.startOnboardingChat()

        viewModel.sendOnboardingMessage("Hola")

        assertEquals("", viewModel.uiState.value.inputText)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `location permission flow continues to AlertsPreferences`() = runTest(StandardTestDispatcher()) {
        val geolocationRepository = FakeGeolocationRepository()
        val viewModel = OnboardingChatViewModel(
            assistant = FakeOnboardingAssistant(GemmaPreparationStatus.Ready),
            geolocationRepository = geolocationRepository,
            deviceLocationProvider = FakeDeviceLocationProvider(Result.success(LatLng(14.0, -90.0))),
        )
        viewModel.startOnboardingChat()
        viewModel.sendOnboardingMessage("Me llamo Juan")
        viewModel.sendOnboardingMessage("Tengo tomate")
        viewModel.sendOnboardingMessage("100m2")
        viewModel.sendOnboardingMessage("Está en crecimiento")

        viewModel.onLocationPermissionResult(granted = true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(OnboardingChatStage.AlertsPreferences, state.onboardingChatStage)
        assertEquals(1, geolocationRepository.reverseGeocodeCalls)
        assertEquals(LatLng(14.0, -90.0), geolocationRepository.lastLatLng)
        assertEquals(true, state.locationShared)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `location permission denied skips geolocation and continues`() = runTest(StandardTestDispatcher()) {
        val geolocationRepository = FakeGeolocationRepository()
        val viewModel = OnboardingChatViewModel(
            assistant = FakeOnboardingAssistant(GemmaPreparationStatus.Ready),
            geolocationRepository = geolocationRepository,
            deviceLocationProvider = FakeDeviceLocationProvider(Result.success(LatLng(14.0, -90.0))),
        )
        viewModel.startOnboardingChat()
        viewModel.sendOnboardingMessage("Me llamo Juan")
        viewModel.sendOnboardingMessage("Tengo tomate")
        viewModel.sendOnboardingMessage("100m2")
        viewModel.sendOnboardingMessage("Está en crecimiento")

        viewModel.onLocationPermissionResult(granted = false)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(OnboardingChatStage.AlertsPreferences, state.onboardingChatStage)
        assertEquals(false, state.locationShared)
        assertEquals(0, geolocationRepository.reverseGeocodeCalls)
    }

    @Test
    fun `completeOnboarding transitions to Final with alerts enabled`() {
        val viewModel = readyViewModel()
        viewModel.startOnboardingChat()
        viewModel.sendOnboardingMessage("Me llamo Juan")
        viewModel.sendOnboardingMessage("Tengo tomate")
        viewModel.sendOnboardingMessage("100m2")
        viewModel.sendOnboardingMessage("Está en crecimiento")
        viewModel.continueOnboardingAfterLocationPermission()
        viewModel.completeOnboarding()

        val state = viewModel.uiState.value
        assertEquals(OnboardingChatStage.Final, state.onboardingChatStage)
        assertEquals(true, state.alertsEnabled)
    }

    @Test
    fun `skipOnboardingAlerts transitions to Final with alerts disabled`() {
        val viewModel = readyViewModel()
        viewModel.startOnboardingChat()
        viewModel.sendOnboardingMessage("Me llamo Juan")
        viewModel.sendOnboardingMessage("Tengo tomate")
        viewModel.sendOnboardingMessage("100m2")
        viewModel.sendOnboardingMessage("Está en crecimiento")
        viewModel.continueOnboardingAfterLocationPermission()
        viewModel.skipOnboardingAlerts()

        val state = viewModel.uiState.value
        assertEquals(OnboardingChatStage.Final, state.onboardingChatStage)
        assertEquals(false, state.alertsEnabled)
    }

    // ========== InputChanged Tests ==========

    @Test
    fun `onInputChanged updates inputText in state`() {
        val viewModel = readyViewModel()

        viewModel.onInputChanged("Hello world")

        assertEquals("Hello world", viewModel.uiState.value.inputText)
    }

    @Test
    fun `onInputChanged with empty text clears input`() {
        val viewModel = readyViewModel()

        viewModel.onInputChanged("Some text")
        viewModel.onInputChanged("")

        assertEquals("", viewModel.uiState.value.inputText)
    }

    // ========== Progress Tests ==========

    @Test
    fun `onboardingProgress is 0 before chat starts`() {
        val viewModel = readyViewModel()
        assertEquals(0f, viewModel.uiState.value.onboardingProgress)
    }

    @Test
    fun `onboardingProgress advances during conversation`() {
        val viewModel = readyViewModel()
        viewModel.startOnboardingChat()

        val progressAfterStart = viewModel.uiState.value.onboardingProgress
        assertTrue(progressAfterStart > 0f)

        viewModel.sendOnboardingMessage("Me llamo Juan")
        val progressAfterFirstExchange = viewModel.uiState.value.onboardingProgress
        assertTrue(progressAfterFirstExchange > progressAfterStart)
    }

    @Test
    fun `onboardingProgress reaches 1f in Final stage`() {
        val viewModel = readyViewModel()
        viewModel.startOnboardingChat()
        viewModel.sendOnboardingMessage("Me llamo Juan")
        viewModel.sendOnboardingMessage("Tengo tomate")
        viewModel.sendOnboardingMessage("100m2")
        viewModel.sendOnboardingMessage("Está en crecimiento")
        viewModel.continueOnboardingAfterLocationPermission()
        viewModel.completeOnboarding()

        assertEquals(1f, viewModel.uiState.value.onboardingProgress)
    }

    // ========== User Data Capture Tests ==========

    @Test
    fun `sendOnboardingMessage step 0 captures userName`() {
        val viewModel = readyViewModel()
        viewModel.startOnboardingChat()

        viewModel.sendOnboardingMessage("Me llamo Juan")

        assertEquals("Juan", viewModel.uiState.value.userName)
    }

    @Test
    fun `sendOnboardingMessage step 1 does not overwrite userName`() {
        val viewModel = readyViewModel()
        viewModel.startOnboardingChat()
        viewModel.sendOnboardingMessage("Me llamo Juan")
        viewModel.sendOnboardingMessage("Tengo tomate")

        assertEquals("Juan", viewModel.uiState.value.userName)
    }

    @Test
    fun `step based flow captures structured onboarding fields`() {
        val viewModel = readyViewModel()
        viewModel.startOnboardingChat()
        viewModel.sendOnboardingMessage("Juan")
        viewModel.sendOnboardingMessage("Tomate y maiz")
        viewModel.sendOnboardingMessage("3 hectareas")
        viewModel.sendOnboardingMessage("Floracion")

        val state = viewModel.uiState.value
        assertEquals("Juan", state.userName)
        assertEquals("Tomate y maiz", state.userCrops)
        assertEquals("3 hectareas", state.userArea)
        assertEquals("Floracion", state.userStage)
    }

    @Test
    fun `single message can capture name crops and area then asks for stage`() {
        val viewModel = readyViewModel()
        viewModel.startOnboardingChat()

        viewModel.sendOnboardingMessage("Me llamo Juan, tengo tomate y maiz en 3 hectareas")

        val state = viewModel.uiState.value
        assertEquals("Juan", state.userName)
        assertEquals("Me llamo Juan, tengo tomate y maiz en 3 hectareas", state.userCrops)
        assertEquals("Me llamo Juan, tengo tomate y maiz en 3 hectareas", state.userArea)
        assertEquals(3, state.onboardingStep)
        assertEquals(OnboardingChatStage.Conversation, state.onboardingChatStage)
        assertTrue(state.messages.last().text.contains("etapa"))
    }

    @Test
    fun `single message can capture all onboarding fields and skip to location permission`() {
        val viewModel = readyViewModel()
        viewModel.startOnboardingChat()

        viewModel.sendOnboardingMessage("Soy Ana, cultivo tomate, 2 ha, etapa de crecimiento")

        val state = viewModel.uiState.value
        assertEquals("Ana", state.userName)
        assertEquals("Soy Ana, cultivo tomate, 2 ha, etapa de crecimiento", state.userCrops)
        assertEquals("Soy Ana, cultivo tomate, 2 ha, etapa de crecimiento", state.userArea)
        assertEquals("Soy Ana, cultivo tomate, 2 ha, etapa de crecimiento", state.userStage)
        assertEquals(4, state.onboardingStep)
        assertEquals(OnboardingChatStage.AwaitingLocationPermission, state.onboardingChatStage)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `location and alerts are captured explicitly in final state`() = runTest(StandardTestDispatcher()) {
        val viewModel = OnboardingChatViewModel(
            assistant = FakeOnboardingAssistant(GemmaPreparationStatus.Ready),
            geolocationRepository = FakeGeolocationRepository(),
            deviceLocationProvider = FakeDeviceLocationProvider(Result.success(LatLng(14.0, -90.0))),
        )
        viewModel.startOnboardingChat()
        viewModel.sendOnboardingMessage("Juan")
        viewModel.sendOnboardingMessage("Tomate")
        viewModel.sendOnboardingMessage("3 hectareas")
        viewModel.sendOnboardingMessage("Floracion")
        viewModel.continueOnboardingAfterLocationPermission()
        advanceUntilIdle()
        viewModel.completeOnboarding(alertsEnabled = false)

        val state = viewModel.uiState.value
        assertEquals(true, state.locationShared)
        assertEquals(true, state.locationEnabled)
        assertEquals(false, state.alertsEnabled)
        assertEquals(OnboardingChatStage.Final, state.onboardingChatStage)
    }
}

private class FakeDeviceLocationProvider(
    private val result: Result<LatLng>,
) : DeviceLocationProvider {
    override suspend fun getCurrentLatLng(): Result<LatLng> = result
}

private class FakeGeolocationRepository : GeolocationRepository {
    var reverseGeocodeCalls: Int = 0
    var lastLatLng: LatLng? = null

    override suspend fun geocode(query: String): Result<GeocodeResolved> =
        Result.failure(UnsupportedOperationException())

    override suspend fun reverseGeocode(latLng: LatLng): Result<ResolvedLocation> {
        reverseGeocodeCalls += 1
        lastLatLng = latLng
        return Result.success(
            ResolvedLocation(
                coordinates = latLng,
                display = LocationDisplay(primary = "Test", municipality = null, state = null, country = null),
                elevationMeters = null,
            ),
        )
    }

    override suspend fun saveResolvedLocation(location: ResolvedLocation) = Unit

    override fun observeResolvedLocation() = flowOf<ResolvedLocation?>(null)
}

private class FakeOnboardingAssistant(
    initialStatus: GemmaPreparationStatus,
) : OnboardingAssistant {
    private val _status = MutableStateFlow(initialStatus)
    override val preparationStatus: StateFlow<GemmaPreparationStatus> = _status

    override suspend fun prepareIfNeeded() = Unit

    override suspend fun reply(step: Int, userText: String, draftState: OnboardingChatUiState): String =
        when (step) {
            1 -> "¿Qué cultivo o cultivos tenés?"
            2 -> "¿Cuántas hectáreas tiene tu cultivo?"
            3 -> "¿En qué etapa está tu cultivo?"
            4 -> "Necesito tu ubicación para darte recomendaciones locales."
            else -> "Seguimos con el onboarding."
        }
}
