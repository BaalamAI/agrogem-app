package com.agrogem.app.data

/**
 * Platform-backed store for the user's selected Gemma model id.
 * Falls back to [GemmaModelOption.Default] when nothing has been saved yet.
 */
expect class GemmaModelPreferenceStore() : GemmaModelPreference {
    override fun read(): GemmaModelOption
    override fun write(id: String)
}
