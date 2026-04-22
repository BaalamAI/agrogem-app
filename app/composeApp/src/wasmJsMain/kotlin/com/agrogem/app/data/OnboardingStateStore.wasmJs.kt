package com.agrogem.app.data

private var onboardingCompleted = false

actual class OnboardingStateStore actual constructor() {
    actual fun isCompleted(): Boolean = onboardingCompleted

    actual fun markCompleted() {
        onboardingCompleted = true
    }
}
