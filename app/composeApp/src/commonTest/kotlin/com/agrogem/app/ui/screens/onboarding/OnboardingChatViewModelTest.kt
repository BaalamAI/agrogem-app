package com.agrogem.app.ui.screens.onboarding

import com.agrogem.app.ui.screens.chat.MessageSender
import com.agrogem.app.ui.screens.onboarding.OnboardingDemoStage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OnboardingChatViewModelTest {

    // ========== Initialization Tests ==========

    @Test
    fun `default initialization starts with empty messages and null stage`() {
        val viewModel = OnboardingChatViewModel()

        val state = viewModel.uiState.value
        assertEquals(emptyList(), state.messages)
        assertEquals("", state.inputText)
        assertEquals(null, state.onboardingDemoStage)
        assertEquals(0, state.onboardingStep)
        assertEquals(false, state.alertsEnabled)
    }

    // ========== Onboarding Demo Step-Based Tests ==========

    @Test
    fun `startOnboardingDemo sets only initial assistant message and step 0`() {
        val viewModel = OnboardingChatViewModel()
        viewModel.startOnboardingDemo()

        val state = viewModel.uiState.value
        assertEquals(1, state.messages.size)
        assertEquals(MessageSender.Assistant, state.messages[0].sender)
        assertTrue(state.messages[0].text.contains("llamás"))
        assertEquals(OnboardingDemoStage.Conversation, state.onboardingDemoStage)
        assertEquals(0, state.onboardingStep)
    }

    @Test
    fun `startOnboardingDemo is idempotent and does not reset existing onboarding state`() {
        val viewModel = OnboardingChatViewModel()
        viewModel.startOnboardingDemo()
        viewModel.sendOnboardingUserMessage("Me llamo Juan")

        val before = viewModel.uiState.value
        assertEquals(3, before.messages.size)
        assertEquals(1, before.onboardingStep)

        viewModel.startOnboardingDemo()

        val after = viewModel.uiState.value
        assertEquals(3, after.messages.size)
        assertEquals(1, after.onboardingStep)
    }

    @Test
    fun `sendOnboardingUserMessage step 0 asks for crops and stays in Conversation`() {
        val viewModel = OnboardingChatViewModel()
        viewModel.startOnboardingDemo()

        viewModel.sendOnboardingUserMessage("Me llamo Juan")

        val state = viewModel.uiState.value
        assertEquals(3, state.messages.size)
        assertEquals(MessageSender.User, state.messages[1].sender)
        assertEquals("Me llamo Juan", state.messages[1].text)
        assertEquals(MessageSender.Assistant, state.messages[2].sender)
        assertTrue(state.messages[2].text.contains("cultivo"))
        assertEquals(OnboardingDemoStage.Conversation, state.onboardingDemoStage)
        assertEquals(1, state.onboardingStep)
    }

    @Test
    fun `sendOnboardingUserMessage step 1 asks for size and stays in Conversation`() {
        val viewModel = OnboardingChatViewModel()
        viewModel.startOnboardingDemo()

        viewModel.sendOnboardingUserMessage("Me llamo Juan")
        viewModel.sendOnboardingUserMessage("Tengo tomate y aguacate")

        val state = viewModel.uiState.value
        assertEquals(5, state.messages.size)
        assertEquals(MessageSender.Assistant, state.messages[4].sender)
        assertTrue(state.messages[4].text.contains("hectáreas") || state.messages[4].text.contains("dimensiones"))
        assertEquals(OnboardingDemoStage.Conversation, state.onboardingDemoStage)
        assertEquals(2, state.onboardingStep)
    }

    @Test
    fun `sendOnboardingUserMessage step 2 asks for stage and stays in Conversation`() {
        val viewModel = OnboardingChatViewModel()
        viewModel.startOnboardingDemo()

        viewModel.sendOnboardingUserMessage("Me llamo Juan")
        viewModel.sendOnboardingUserMessage("Tengo tomate")
        viewModel.sendOnboardingUserMessage("100m2")

        val state = viewModel.uiState.value
        assertEquals(7, state.messages.size)
        assertEquals(MessageSender.Assistant, state.messages[6].sender)
        assertTrue(state.messages[6].text.contains("etapa") || state.messages[6].text.contains("Alistando"))
        assertEquals(OnboardingDemoStage.Conversation, state.onboardingDemoStage)
        assertEquals(3, state.onboardingStep)
    }

    @Test
    fun `sendOnboardingUserMessage step 3 asks for location and transitions to AwaitingLocationPermission`() {
        val viewModel = OnboardingChatViewModel()
        viewModel.startOnboardingDemo()

        viewModel.sendOnboardingUserMessage("Me llamo Juan")
        viewModel.sendOnboardingUserMessage("Tengo tomate")
        viewModel.sendOnboardingUserMessage("100m2")
        viewModel.sendOnboardingUserMessage("Está en crecimiento")

        val state = viewModel.uiState.value
        assertEquals(9, state.messages.size)
        assertEquals(MessageSender.Assistant, state.messages[8].sender)
        assertTrue(state.messages[8].text.contains("ubicación") || state.messages[8].text.contains("zona"))
        assertEquals(OnboardingDemoStage.AwaitingLocationPermission, state.onboardingDemoStage)
        assertEquals(4, state.onboardingStep)
    }

    @Test
    fun `sendOnboardingUserMessage does nothing when demo has not started`() {
        val viewModel = OnboardingChatViewModel()
        // Default stage is null, demo not started

        viewModel.sendOnboardingUserMessage("Hola")

        assertEquals(emptyList(), viewModel.uiState.value.messages)
    }

    @Test
    fun `sendOnboardingUserMessage does nothing when not in Conversation stage`() {
        val viewModel = OnboardingChatViewModel()
        viewModel.startOnboardingDemo()
        viewModel.sendOnboardingUserMessage("Me llamo Juan")
        viewModel.sendOnboardingUserMessage("Tengo tomate")
        viewModel.sendOnboardingUserMessage("100m2")
        viewModel.sendOnboardingUserMessage("Está en crecimiento")

        // Now in AwaitingLocationPermission
        assertEquals(OnboardingDemoStage.AwaitingLocationPermission, viewModel.uiState.value.onboardingDemoStage)

        viewModel.sendOnboardingUserMessage("Mensaje extra")

        // Should not add more messages
        assertEquals(9, viewModel.uiState.value.messages.size)
    }

    @Test
    fun `sendOnboardingUserMessage ignores empty input`() {
        val viewModel = OnboardingChatViewModel()
        viewModel.startOnboardingDemo()

        viewModel.sendOnboardingUserMessage("   ")

        assertEquals(1, viewModel.uiState.value.messages.size)
    }

    @Test
    fun `sendOnboardingUserMessage clears inputText`() {
        val viewModel = OnboardingChatViewModel()
        viewModel.startOnboardingDemo()

        viewModel.sendOnboardingUserMessage("Hola")

        assertEquals("", viewModel.uiState.value.inputText)
    }

    @Test
    fun `location permission flow continues to AlertsPreferences`() {
        val viewModel = OnboardingChatViewModel()
        viewModel.startOnboardingDemo()
        viewModel.sendOnboardingUserMessage("Me llamo Juan")
        viewModel.sendOnboardingUserMessage("Tengo tomate")
        viewModel.sendOnboardingUserMessage("100m2")
        viewModel.sendOnboardingUserMessage("Está en crecimiento")

        viewModel.continueOnboardingDemoAfterLocationPermission()

        val state = viewModel.uiState.value
        assertEquals(OnboardingDemoStage.AlertsPreferences, state.onboardingDemoStage)
    }

    @Test
    fun `completeOnboardingDemo transitions to Final with alerts enabled`() {
        val viewModel = OnboardingChatViewModel()
        viewModel.startOnboardingDemo()
        viewModel.sendOnboardingUserMessage("Me llamo Juan")
        viewModel.sendOnboardingUserMessage("Tengo tomate")
        viewModel.sendOnboardingUserMessage("100m2")
        viewModel.sendOnboardingUserMessage("Está en crecimiento")
        viewModel.continueOnboardingDemoAfterLocationPermission()
        viewModel.completeOnboardingDemo()

        val state = viewModel.uiState.value
        assertEquals(OnboardingDemoStage.Final, state.onboardingDemoStage)
        assertEquals(true, state.alertsEnabled)
    }

    @Test
    fun `skipOnboardingAlerts transitions to Final with alerts disabled`() {
        val viewModel = OnboardingChatViewModel()
        viewModel.startOnboardingDemo()
        viewModel.sendOnboardingUserMessage("Me llamo Juan")
        viewModel.sendOnboardingUserMessage("Tengo tomate")
        viewModel.sendOnboardingUserMessage("100m2")
        viewModel.sendOnboardingUserMessage("Está en crecimiento")
        viewModel.continueOnboardingDemoAfterLocationPermission()
        viewModel.skipOnboardingAlerts()

        val state = viewModel.uiState.value
        assertEquals(OnboardingDemoStage.Final, state.onboardingDemoStage)
        assertEquals(false, state.alertsEnabled)
    }

    // ========== InputChanged Tests ==========

    @Test
    fun `onInputChanged updates inputText in state`() {
        val viewModel = OnboardingChatViewModel()

        viewModel.onInputChanged("Hello world")

        assertEquals("Hello world", viewModel.uiState.value.inputText)
    }

    @Test
    fun `onInputChanged with empty text clears input`() {
        val viewModel = OnboardingChatViewModel()

        viewModel.onInputChanged("Some text")
        viewModel.onInputChanged("")

        assertEquals("", viewModel.uiState.value.inputText)
    }

    // ========== Progress Tests ==========

    @Test
    fun `onboardingProgress is 0 before demo starts`() {
        val viewModel = OnboardingChatViewModel()
        assertEquals(0f, viewModel.uiState.value.onboardingProgress)
    }

    @Test
    fun `onboardingProgress advances during conversation`() {
        val viewModel = OnboardingChatViewModel()
        viewModel.startOnboardingDemo()

        val progressAfterStart = viewModel.uiState.value.onboardingProgress
        assertTrue(progressAfterStart > 0f)

        viewModel.sendOnboardingUserMessage("Me llamo Juan")
        val progressAfterFirstExchange = viewModel.uiState.value.onboardingProgress
        assertTrue(progressAfterFirstExchange > progressAfterStart)
    }

    @Test
    fun `onboardingProgress reaches 1f in Final stage`() {
        val viewModel = OnboardingChatViewModel()
        viewModel.startOnboardingDemo()
        viewModel.sendOnboardingUserMessage("Me llamo Juan")
        viewModel.sendOnboardingUserMessage("Tengo tomate")
        viewModel.sendOnboardingUserMessage("100m2")
        viewModel.sendOnboardingUserMessage("Está en crecimiento")
        viewModel.continueOnboardingDemoAfterLocationPermission()
        viewModel.completeOnboardingDemo()

        assertEquals(1f, viewModel.uiState.value.onboardingProgress)
    }
}
