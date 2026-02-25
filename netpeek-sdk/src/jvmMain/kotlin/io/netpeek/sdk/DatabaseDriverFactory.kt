package io.netpeek.sdk

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.netpeek.db.NetPeekDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver("jdbc:sqlite:netpeek.db")
        NetPeekDatabase.Schema.create(driver)
        return driver
    }
}
