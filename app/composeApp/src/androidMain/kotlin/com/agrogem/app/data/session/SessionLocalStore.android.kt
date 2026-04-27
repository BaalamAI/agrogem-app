package com.agrogem.app.data.session

import android.content.Context.MODE_PRIVATE
import com.agrogem.app.AndroidAppContext

private const val PREFS_NAME = "agrogem_session"
private const val KEY_ONBOARDING_DONE = "onboarding_done"
private const val KEY_PHONE = "phone"
private const val KEY_SESSION_ID = "session_id"

actual class SessionLocalStore actual constructor() {
    private var previewFallback = SessionSnapshot()

    private fun prefsOrNull() =
        if (AndroidAppContext.isInitialized) {
            AndroidAppContext.context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        } else {
            null
        }

    actual suspend fun read(): SessionSnapshot {
        val prefs = prefsOrNull()
        return if (prefs != null) {
            SessionSnapshot(
                onboardingDone = prefs.getBoolean(KEY_ONBOARDING_DONE, false),
                phone = prefs.getString(KEY_PHONE, null),
                sessionId = prefs.getString(KEY_SESSION_ID, null)
            )
        } else {
            previewFallback
        }
    }

    actual suspend fun write(snapshot: SessionSnapshot) {
        val prefs = prefsOrNull()
        if (prefs != null) {
            prefs.edit()
                .putBoolean(KEY_ONBOARDING_DONE, snapshot.onboardingDone)
                .putString(KEY_PHONE, snapshot.phone)
                .putString(KEY_SESSION_ID, snapshot.sessionId)
                .apply()
        } else {
            previewFallback = snapshot
        }
    }

    actual suspend fun clearSession() {
        val prefs = prefsOrNull()
        if (prefs != null) {
            prefs.edit()
                .remove(KEY_ONBOARDING_DONE)
                .remove(KEY_PHONE)
                .remove(KEY_SESSION_ID)
                .apply()
        } else {
            previewFallback = SessionSnapshot()
        }
    }
}
