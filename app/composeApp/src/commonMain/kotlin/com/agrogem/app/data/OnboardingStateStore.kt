package com.agrogem.app.data

/**
 * Persists whether the onboarding flow was completed at least once.
 */
expect class OnboardingStateStore() {
    fun isCompleted(): Boolean
    fun markCompleted()
}
