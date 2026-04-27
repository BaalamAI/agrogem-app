package com.agrogem.app.data.session

import platform.Foundation.NSUserDefaults

private const val KEY_ONBOARDING_DONE = "onboarding_done"
private const val KEY_PHONE = "phone"
private const val KEY_SESSION_ID = "session_id"

actual class SessionLocalStore actual constructor() {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual suspend fun read(): SessionSnapshot =
        SessionSnapshot(
            onboardingDone = defaults.boolForKey(KEY_ONBOARDING_DONE),
            phone = defaults.stringForKey(KEY_PHONE),
            sessionId = defaults.stringForKey(KEY_SESSION_ID)
        )

    actual suspend fun write(snapshot: SessionSnapshot) {
        defaults.setBool(snapshot.onboardingDone, forKey = KEY_ONBOARDING_DONE)
        snapshot.phone?.let { defaults.setObject(it, forKey = KEY_PHONE) }
            ?: defaults.removeObjectForKey(KEY_PHONE)
        snapshot.sessionId?.let { defaults.setObject(it, forKey = KEY_SESSION_ID) }
            ?: defaults.removeObjectForKey(KEY_SESSION_ID)
    }

    actual suspend fun clearSession() {
        defaults.removeObjectForKey(KEY_ONBOARDING_DONE)
        defaults.removeObjectForKey(KEY_PHONE)
        defaults.removeObjectForKey(KEY_SESSION_ID)
    }
}
