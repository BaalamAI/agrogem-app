package com.agrogem.app.data.connectivity

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_queue_create

actual fun createConnectivityMonitor(): ConnectivityMonitor = IosConnectivityMonitor()

private class IosConnectivityMonitor : ConnectivityMonitor {
    private val _isOnline = MutableStateFlow(true)

    init {
        startMonitoring()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun startMonitoring() {
        val monitor = nw_path_monitor_create()
        val queue = dispatch_queue_create("connectivity_monitor", null)

        nw_path_monitor_set_update_handler(monitor) { path ->
            val status = nw_path_get_status(path)
            _isOnline.value = (status == nw_path_status_satisfied)
        }

        nw_path_monitor_set_queue(monitor, queue)
        nw_path_monitor_start(monitor)
    }

    override fun isOnline(): Boolean = _isOnline.value
}
