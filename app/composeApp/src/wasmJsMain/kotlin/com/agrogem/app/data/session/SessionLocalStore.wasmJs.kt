package com.agrogem.app.data.session

import kotlinx.browser.localStorage

private const val KEY_ONBOARDING_DONE = "onboarding_done"
private const val KEY_PHONE = "phone"
private const val KEY_SESSION_ID = "session_id"

actual class SessionLocalStore actual constructor() {
    private var memoryFallback = SessionSnapshot()
    private val storageAvailable = try {
        localStorage
        true
    } catch (_: Throwable) {
        false
    }

    actual suspend fun read(): SessionSnapshot {
        if (!storageAvailable) return memoryFallback
        return try {
            SessionSnapshot(
                onboardingDone = localStorage.getItem(KEY_ONBOARDING_DONE) == "true",
                phone = localStorage.getItem(KEY_PHONE),
                sessionId = localStorage.getItem(KEY_SESSION_ID)
            )
        } catch (_: Throwable) {
            memoryFallback
        }
    }

    actual suspend fun write(snapshot: SessionSnapshot) {
        if (!storageAvailable) {
            memoryFallback = snapshot
            return
        }
        try {
            localStorage.setItem(KEY_ONBOARDING_DONE, snapshot.onboardingDone.toString())
            if (snapshot.phone != null) {
                localStorage.setItem(KEY_PHONE, snapshot.phone)
            } else {
                localStorage.removeItem(KEY_PHONE)
            }
            if (snapshot.sessionId != null) {
                localStorage.setItem(KEY_SESSION_ID, snapshot.sessionId)
            } else {
                localStorage.removeItem(KEY_SESSION_ID)
            }
        } catch (_: Throwable) {
            memoryFallback = snapshot
        }
    }

    actual suspend fun clearSession() {
        if (!storageAvailable) {
            memoryFallback = SessionSnapshot()
            return
        }
        try {
            localStorage.removeItem(KEY_ONBOARDING_DONE)
            localStorage.removeItem(KEY_PHONE)
            localStorage.removeItem(KEY_SESSION_ID)
        } catch (_: Throwable) {
            memoryFallback = SessionSnapshot()
        }
    }
}
