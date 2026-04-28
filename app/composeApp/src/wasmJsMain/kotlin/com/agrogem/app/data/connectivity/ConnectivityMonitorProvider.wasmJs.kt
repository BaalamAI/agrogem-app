package com.agrogem.app.data.connectivity

import kotlinx.browser.window

actual fun createConnectivityMonitor(): ConnectivityMonitor = WasmJsConnectivityMonitor()

private class WasmJsConnectivityMonitor : ConnectivityMonitor {
    override fun isOnline(): Boolean = window.navigator.onLine
}
