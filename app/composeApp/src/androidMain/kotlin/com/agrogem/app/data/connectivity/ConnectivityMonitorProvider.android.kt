package com.agrogem.app.data.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.agrogem.app.AndroidAppContext

actual fun createConnectivityMonitor(): ConnectivityMonitor = AndroidConnectivityMonitor()

private class AndroidConnectivityMonitor : ConnectivityMonitor {
    override fun isOnline(): Boolean {
        val context = AndroidAppContext.context
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
