package com.agrogem.app

import android.content.Context

object AndroidAppContext {
    lateinit var context: Context

    val isInitialized: Boolean
        get() = this::context.isInitialized
}
