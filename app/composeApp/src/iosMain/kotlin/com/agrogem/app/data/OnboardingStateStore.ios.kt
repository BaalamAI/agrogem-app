package com.agrogem.app.data

import platform.Foundation.NSUserDefaults

private const val KEY_ONBOARDING_DONE = "onboarding_done"

actual class OnboardingStateStore actual constructor() {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual val isCompleted: Boolean
        get() = defaults.boolForKey(KEY_ONBOARDING_DONE)

    actual fun markCompleted() {
        defaults.setBool(true, forKey = KEY_ONBOARDING_DONE)
    }
}
