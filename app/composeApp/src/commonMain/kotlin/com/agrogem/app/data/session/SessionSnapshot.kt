package com.agrogem.app.data.session

/**
 * Immutable snapshot of the user's session and onboarding state.
 */
data class SessionSnapshot(
    val onboardingDone: Boolean = false,
    val phone: String? = null,
    val sessionId: String? = null
)
