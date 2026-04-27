package com.agrogem.app.ui.screens.onboarding

import com.agrogem.app.ui.screens.chat.MessageSender
import com.agrogem.app.ui.screens.onboarding.OnboardingChatStage
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
        assertEquals(null, state.onboardingChatStage)
        assertEquals(0, state.onboardingStep)
        assertEquals(false, state.alertsEnabled)
    }

    // ========== Onboarding Demo Step-Based Tests ==========

    @Test
    fun `startOnboardingChat sets only initial assistant message and step 0`() {
        val viewModel = OnboardingChatViewModel()
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
        val viewModel = OnboardingChatViewModel()
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
        val viewModel = OnboardingChatViewModel()
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
        val viewModel = OnboardingChatViewModel()
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
        val viewModel = OnboardingChatViewModel()
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
        val viewModel = OnboardingChatViewModel()
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
        val viewModel = OnboardingChatViewModel()
        // Default stage is null, chat not started

        viewModel.sendOnboardingMessage("Hola")

        assertEquals(emptyList(), viewModel.uiState.value.messages)
    }

    @Test
    fun `sendOnboardingMessage does nothing when not in Conversation stage`() {
        val viewModel = OnboardingChatViewModel()
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
        val viewModel = OnboardingChatViewModel()
        viewModel.startOnboardingChat()

        viewModel.sendOnboardingMessage("   ")

        assertEquals(1, viewModel.uiState.value.messages.size)
    }

    @Test
    fun `sendOnboardingMessage clears inputText`() {
        val viewModel = OnboardingChatViewModel()
        viewModel.startOnboardingChat()

        viewModel.sendOnboardingMessage("Hola")

        assertEquals("", viewModel.uiState.value.inputText)
    }

    @Test
    fun `location permission flow continues to AlertsPreferences`() {
        val viewModel = OnboardingChatViewModel()
        viewModel.startOnboardingChat()
        viewModel.sendOnboardingMessage("Me llamo Juan")
        viewModel.sendOnboardingMessage("Tengo tomate")
        viewModel.sendOnboardingMessage("100m2")
        viewModel.sendOnboardingMessage("Está en crecimiento")

        viewModel.continueOnboardingAfterLocationPermission()

        val state = viewModel.uiState.value
        assertEquals(OnboardingChatStage.AlertsPreferences, state.onboardingChatStage)
    }

    @Test
    fun `completeOnboarding transitions to Final with alerts enabled`() {
        val viewModel = OnboardingChatViewModel()
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
        val viewModel = OnboardingChatViewModel()
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
    fun `onboardingProgress is 0 before chat starts`() {
        val viewModel = OnboardingChatViewModel()
        assertEquals(0f, viewModel.uiState.value.onboardingProgress)
    }

    @Test
    fun `onboardingProgress advances during conversation`() {
        val viewModel = OnboardingChatViewModel()
        viewModel.startOnboardingChat()

        val progressAfterStart = viewModel.uiState.value.onboardingProgress
        assertTrue(progressAfterStart > 0f)

        viewModel.sendOnboardingMessage("Me llamo Juan")
        val progressAfterFirstExchange = viewModel.uiState.value.onboardingProgress
        assertTrue(progressAfterFirstExchange > progressAfterStart)
    }

    @Test
    fun `onboardingProgress reaches 1f in Final stage`() {
        val viewModel = OnboardingChatViewModel()
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
        val viewModel = OnboardingChatViewModel()
        viewModel.startOnboardingChat()

        viewModel.sendOnboardingMessage("Me llamo Juan")

        assertEquals("Me llamo Juan", viewModel.uiState.value.userName)
    }

    @Test
    fun `sendOnboardingMessage step 1 does not overwrite userName`() {
        val viewModel = OnboardingChatViewModel()
        viewModel.startOnboardingChat()
        viewModel.sendOnboardingMessage("Me llamo Juan")
        viewModel.sendOnboardingMessage("Tengo tomate")

        assertEquals("Me llamo Juan", viewModel.uiState.value.userName)
    }

    @Test
    fun `onPhoneChanged updates userPhone`() {
        val viewModel = OnboardingChatViewModel()

        viewModel.onPhoneChanged("+50255550000")

        assertEquals("+50255550000", viewModel.uiState.value.userPhone)
    }

    @Test
    fun `onPhoneChanged with empty string clears userPhone`() {
        val viewModel = OnboardingChatViewModel()

        viewModel.onPhoneChanged("+50255550000")
        viewModel.onPhoneChanged("")

        assertEquals("", viewModel.uiState.value.userPhone)
    }

    @Test
    fun `onPasswordChanged updates userPassword`() {
        val viewModel = OnboardingChatViewModel()

        viewModel.onPasswordChanged("secret123")

        assertEquals("secret123", viewModel.uiState.value.userPassword)
    }

    @Test
    fun `onPasswordChanged with empty string clears userPassword`() {
        val viewModel = OnboardingChatViewModel()

        viewModel.onPasswordChanged("secret123")
        viewModel.onPasswordChanged("")

        assertEquals("", viewModel.uiState.value.userPassword)
    }

    @Test
    fun `isPhoneValid returns true for valid phone`() {
        val viewModel = OnboardingChatViewModel()
        viewModel.onPhoneChanged("+50255550000")

        assertEquals(true, viewModel.uiState.value.isPhoneValid)
    }

    @Test
    fun `isPhoneValid returns false for too short phone`() {
        val viewModel = OnboardingChatViewModel()
        viewModel.onPhoneChanged("+5025")

        assertEquals(false, viewModel.uiState.value.isPhoneValid)
    }

    @Test
    fun `isPasswordValid returns true for 8 char password`() {
        val viewModel = OnboardingChatViewModel()
        viewModel.onPasswordChanged("abcdefgh")

        assertEquals(true, viewModel.uiState.value.isPasswordValid)
    }

    @Test
    fun `isPasswordValid returns false for short password`() {
        val viewModel = OnboardingChatViewModel()
        viewModel.onPasswordChanged("short")

        assertEquals(false, viewModel.uiState.value.isPasswordValid)
    }

    @Test
    fun `isFormValid requires both phone and password`() {
        val viewModel = OnboardingChatViewModel()
        viewModel.onPhoneChanged("+50255550000")
        viewModel.onPasswordChanged("abcdefgh")

        assertEquals(true, viewModel.uiState.value.isFormValid)
    }

    @Test
    fun `isFormValid is false when only phone is valid`() {
        val viewModel = OnboardingChatViewModel()
        viewModel.onPhoneChanged("+50255550000")
        viewModel.onPasswordChanged("short")

        assertEquals(false, viewModel.uiState.value.isFormValid)
    }

    @Test
    fun `isFormValid is false when only password is valid`() {
        val viewModel = OnboardingChatViewModel()
        viewModel.onPhoneChanged("+5025")
        viewModel.onPasswordChanged("abcdefgh")

        assertEquals(false, viewModel.uiState.value.isFormValid)
    }
}
