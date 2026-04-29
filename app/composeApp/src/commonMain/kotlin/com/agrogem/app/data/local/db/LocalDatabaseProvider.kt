package com.agrogem.app.data.local.db

object LocalDatabaseProvider {
    val database: AgroGemDatabase by lazy {
        AgroGemDatabase(
            driver = DatabaseDriverFactory().createDriver(),
        )
    }
}
