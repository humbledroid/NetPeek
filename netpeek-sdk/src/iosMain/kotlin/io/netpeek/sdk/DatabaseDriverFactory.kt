package io.netpeek.sdk

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import io.netpeek.db.NetPeekDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(NetPeekDatabase.Schema, "netpeek.db")
    }
}
