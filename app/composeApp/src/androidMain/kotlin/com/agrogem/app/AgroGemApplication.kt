package com.agrogem.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.agrogem.app.service.InferenceForegroundService

/**
 * Application subclass — runs before any Activity, so it's the safest place to
 * (a) initialize the global [AndroidAppContext] used by KMP singletons, and
 * (b) register the notification channel the foreground service relies on.
 *
 * Per Android docs, posting a foreground-service notification against a channel
 * that hasn't been registered first throws RemoteServiceException at startForeground —
 * so the channel MUST exist before the service tries to start. Doing it here
 * (rather than in the service's onCreate) guarantees that ordering.
 */
class AgroGemApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidAppContext.context = applicationContext
        registerInferenceChannel()
    }

    private fun registerInferenceChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = getSystemService(NotificationManager::class.java) ?: return
        if (mgr.getNotificationChannel(InferenceForegroundService.CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            InferenceForegroundService.CHANNEL_ID,
            "Inferencia AgroGem",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Avisa cuando AgroGem está procesando una consulta o análisis."
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        mgr.createNotificationChannel(channel)
    }
}
