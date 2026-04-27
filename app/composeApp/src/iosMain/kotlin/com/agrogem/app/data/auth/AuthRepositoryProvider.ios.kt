package com.agrogem.app.data.auth

import com.agrogem.app.data.auth.api.KtorAuthApi
import com.agrogem.app.data.auth.domain.AuthRepository
import com.agrogem.app.data.auth.domain.AuthRepositoryImpl
import com.agrogem.app.data.network.HttpClientFactory
import com.agrogem.app.data.session.SessionLocalStore
import io.ktor.client.engine.darwin.Darwin

actual fun createAuthRepository(store: SessionLocalStore): AuthRepository {
    val client = HttpClientFactory.create(engine = Darwin.create())
    val api = KtorAuthApi(client)
    return AuthRepositoryImpl(api = api, store = store)
}
