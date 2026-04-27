package com.agrogem.app.data.session

/**
 * Platform-local persistence for session snapshots.
 */
expect class SessionLocalStore() {
    suspend fun read(): SessionSnapshot
    suspend fun write(snapshot: SessionSnapshot)
    suspend fun clearSession()
}
