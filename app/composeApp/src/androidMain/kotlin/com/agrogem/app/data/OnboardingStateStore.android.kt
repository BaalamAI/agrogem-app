package com.agrogem.app.data

import android.content.Context.MODE_PRIVATE
import com.agrogem.app.AndroidAppContext

private const val PREFS_NAME = "agrogem_onboarding"
private const val KEY_ONBOARDING_DONE = "onboarding_done"

actual class OnboardingStateStore actual constructor() {
    private val previewFallback = object {
        var completed = false
    }

    private fun prefsOrNull() =
        if (AndroidAppContext.isInitialized) {
            AndroidAppContext.context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        } else {
            null
        }

    actual fun isCompleted(): Boolean =
        prefsOrNull()?.getBoolean(KEY_ONBOARDING_DONE, false) ?: previewFallback.completed

    actual fun markCompleted() {
        val prefs = prefsOrNull()
        if (prefs != null) {
            prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
        } else {
            previewFallback.completed = true
        }
    }
}
