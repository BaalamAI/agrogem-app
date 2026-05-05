package com.agrogem.app.data.location

import kotlin.concurrent.Volatile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

object LocationGate {
    private val requestChannel = Channel<Unit>(capacity = Channel.CONFLATED)
    val requests: Flow<Unit> = requestChannel.receiveAsFlow()

    @Volatile
    private var pending: CompletableDeferred<Boolean>? = null

    suspend fun requestPermission(): Boolean {
        pending?.let { return it.await() }
        val deferred = CompletableDeferred<Boolean>()
        pending = deferred
        requestChannel.trySend(Unit)
        return try {
            deferred.await()
        } finally {
            if (pending === deferred) pending = null
        }
    }

    fun onPermissionResult(granted: Boolean) {
        pending?.complete(granted)
    }
}
