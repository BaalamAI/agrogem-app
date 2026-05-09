package com.agrogem.app.data

import androidx.core.content.ContextCompat
import com.agrogem.app.AndroidAppContext
import com.agrogem.app.service.InferenceForegroundService
import java.util.concurrent.atomic.AtomicInteger

actual object InferenceLifecycle {
    private val refCount = AtomicInteger(0)

    actual fun start(label: String) {
        if (!AndroidAppContext.isInitialized) return
        val context = AndroidAppContext.context
        // Always send the start intent so the notification text gets updated to
        // the latest label, even if a prior caller already started the service.
        // ContextCompat handles the API 26+ requirement to use startForegroundService.
        ContextCompat.startForegroundService(
            context,
            InferenceForegroundService.newStartIntent(context, label),
        )
        val now = refCount.incrementAndGet()
        InferenceForegroundService.logState("start label='$label' refCount=$now")
    }

    actual fun stop() {
        if (!AndroidAppContext.isInitialized) return
        val now = refCount.decrementAndGet()
        InferenceForegroundService.logState("stop refCount=$now")
        if (now <= 0) {
            // Clamp to zero in case stop was called more times than start —
            // benign in practice, but keeps the counter sane for the next session.
            refCount.set(0)
            val context = AndroidAppContext.context
            context.stopService(InferenceForegroundService.newStopIntent(context))
        }
    }
}
