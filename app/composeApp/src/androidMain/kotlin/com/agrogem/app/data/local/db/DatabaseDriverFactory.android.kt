package com.agrogem.app.data.local.db

import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.cash.sqldelight.db.SqlDriver
import com.agrogem.app.AndroidAppContext

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(
            schema = AgroGemDatabase.Schema,
            context = AndroidAppContext.context,
            name = "agrogem.db",
        )
}
