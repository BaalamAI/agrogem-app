package com.agrogem.app.util

actual fun platformLog(tag: String, message: String) {
    println("[$tag] $message")
}
