package com.agrogem.app.data

// iOS POC was dropped; foreground-service equivalents would require BGTaskScheduler
// and audio/processing background modes. Stubs keep KMP compilation happy without
// committing to a platform we no longer ship.
actual object InferenceLifecycle {
    actual fun start(label: String) {}
    actual fun stop() {}
}
