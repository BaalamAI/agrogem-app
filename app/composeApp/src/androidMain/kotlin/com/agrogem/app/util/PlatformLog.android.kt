package com.agrogem.app.util

import android.util.Log

actual fun platformLog(tag: String, message: String) {
    runCatching { Log.i(tag, message) }
}
