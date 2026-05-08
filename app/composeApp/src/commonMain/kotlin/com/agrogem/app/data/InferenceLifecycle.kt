package com.agrogem.app.data

/**
 * Brackets a long-running on-device inference (Gemma chat or pest analysis) so
 * the platform can elevate the process priority and protect it from the OS
 * killing it under memory pressure.
 *
 * On Android this drives a foreground service with a persistent notification.
 * On iOS this is a no-op (iOS is dropped from the POC).
 *
 * Always call [stop] in a `finally` block — coroutine cancellation must release
 * the foreground state, otherwise a long-lived notification will outlive the
 * inference it was protecting.
 *
 * The implementation is reference-counted, so concurrent chat and pest-analysis
 * inferences each own one acquire/release pair without stepping on each other.
 */
expect object InferenceLifecycle {
    /**
     * Marks an inference as started. Safe to nest — internal refcount tracks
     * concurrent acquires. [label] updates the platform notification text so
     * the user sees what the app is currently doing.
     */
    fun start(label: String)

    /** Marks an inference as finished. Pair every [start] with exactly one [stop]. */
    fun stop()
}
