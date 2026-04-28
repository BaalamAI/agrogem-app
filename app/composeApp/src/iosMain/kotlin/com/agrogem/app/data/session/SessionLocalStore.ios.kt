package com.agrogem.app.data.session

import platform.Foundation.NSUserDefaults

private const val KEY_ONBOARDING_DONE = "onboarding_done"
private const val KEY_PHONE = "phone"
private const val KEY_SESSION_ID = "session_id"
private const val KEY_NAME = "name"
private const val KEY_CROPS = "crops"
private const val KEY_AREA = "area"
private const val KEY_STAGE = "stage"

actual class SessionLocalStore actual constructor() {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual suspend fun read(): SessionSnapshot =
        SessionSnapshot(
            onboardingDone = defaults.boolForKey(KEY_ONBOARDING_DONE),
            phone = defaults.stringForKey(KEY_PHONE),
            sessionId = defaults.stringForKey(KEY_SESSION_ID),
            name = defaults.stringForKey(KEY_NAME),
            crops = defaults.stringForKey(KEY_CROPS),
            area = defaults.stringForKey(KEY_AREA),
            stage = defaults.stringForKey(KEY_STAGE),
        )

    actual suspend fun write(snapshot: SessionSnapshot) {
        defaults.setBool(snapshot.onboardingDone, forKey = KEY_ONBOARDING_DONE)
        snapshot.phone?.let { defaults.setObject(it, forKey = KEY_PHONE) }
            ?: defaults.removeObjectForKey(KEY_PHONE)
        snapshot.sessionId?.let { defaults.setObject(it, forKey = KEY_SESSION_ID) }
            ?: defaults.removeObjectForKey(KEY_SESSION_ID)
        snapshot.name?.let { defaults.setObject(it, forKey = KEY_NAME) }
            ?: defaults.removeObjectForKey(KEY_NAME)
        snapshot.crops?.let { defaults.setObject(it, forKey = KEY_CROPS) }
            ?: defaults.removeObjectForKey(KEY_CROPS)
        snapshot.area?.let { defaults.setObject(it, forKey = KEY_AREA) }
            ?: defaults.removeObjectForKey(KEY_AREA)
        snapshot.stage?.let { defaults.setObject(it, forKey = KEY_STAGE) }
            ?: defaults.removeObjectForKey(KEY_STAGE)
    }

    actual suspend fun clearSession() {
        defaults.removeObjectForKey(KEY_ONBOARDING_DONE)
        defaults.removeObjectForKey(KEY_PHONE)
        defaults.removeObjectForKey(KEY_SESSION_ID)
        defaults.removeObjectForKey(KEY_NAME)
        defaults.removeObjectForKey(KEY_CROPS)
        defaults.removeObjectForKey(KEY_AREA)
        defaults.removeObjectForKey(KEY_STAGE)
    }
}
