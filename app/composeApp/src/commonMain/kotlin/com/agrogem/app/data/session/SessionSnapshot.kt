package com.agrogem.app.data.session

/**
 * Immutable snapshot of the user's session and onboarding state.
 */
data class SessionSnapshot(
    val onboardingDone: Boolean = false,
    val phone: String? = null,
    val sessionId: String? = null,
    val name: String? = null,
    val crops: String? = null,
    val area: String? = null,
    val stage: String? = null,
)
