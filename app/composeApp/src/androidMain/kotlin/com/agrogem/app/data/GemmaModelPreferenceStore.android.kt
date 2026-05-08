package com.agrogem.app.data

import android.content.Context.MODE_PRIVATE
import com.agrogem.app.AndroidAppContext

private const val PREFS_NAME = "agrogem_model_preference"
private const val KEY_MODEL_ID = "selected_model_id"

actual class GemmaModelPreferenceStore actual constructor() : GemmaModelPreference {
    private fun prefsOrNull() =
        if (AndroidAppContext.isInitialized) {
            AndroidAppContext.context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        } else {
            null
        }

    actual override fun read(): GemmaModelOption {
        val id = prefsOrNull()?.getString(KEY_MODEL_ID, null)
        return GemmaModelOption.fromId(id)
    }

    actual override fun write(id: String) {
        prefsOrNull()?.edit()?.putString(KEY_MODEL_ID, id)?.apply()
    }
}
