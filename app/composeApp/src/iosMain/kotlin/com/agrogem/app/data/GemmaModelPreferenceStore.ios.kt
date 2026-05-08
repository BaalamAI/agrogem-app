package com.agrogem.app.data

import platform.Foundation.NSUserDefaults

private const val KEY_MODEL_ID = "selected_model_id"

actual class GemmaModelPreferenceStore actual constructor() : GemmaModelPreference {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual override fun read(): GemmaModelOption {
        val id = defaults.stringForKey(KEY_MODEL_ID)
        return GemmaModelOption.fromId(id)
    }

    actual override fun write(id: String) {
        defaults.setObject(id, forKey = KEY_MODEL_ID)
    }
}
