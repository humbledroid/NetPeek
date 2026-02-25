package io.netpeek.sdk

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import io.netpeek.db.NetPeekDatabase
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class NetworkCallRepositoryImpl(driverFactory: DatabaseDriverFactory) : NetworkCallRepository {

    private val db = NetPeekDatabase(driverFactory.createDriver())
    private val queries = db.networkCallsQueries

    override fun getAllCalls(): Flow<List<NetworkCall>> {
        return queries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toNetworkCall() } }
    }

    override suspend fun getCallById(id: Long): NetworkCall? = withContext(Dispatchers.Default) {
        queries.selectById(id).executeAsOneOrNull()?.toNetworkCall()
    }

    override suspend fun insert(call: NetworkCall) = withContext(Dispatchers.Default) {
        queries.insertCall(
            url = call.url,
            method = call.method,
            request_headers = call.requestHeaders,
            request_body = call.requestBody,
            response_code = call.responseCode?.toLong(),
            response_headers = call.responseHeaders,
            response_body = call.responseBody,
            duration_ms = call.durationMs,
            timestamp = call.timestamp,
            is_error = if (call.isError) 1L else 0L
        )
    }

    override suspend fun clearAll() = withContext(Dispatchers.Default) {
        queries.deleteAll()
    }

    override suspend fun count(): Long = withContext(Dispatchers.Default) {
        queries.countAll().executeAsOne()
    }

    private fun io.netpeek.db.Network_calls.toNetworkCall() = NetworkCall(
        id = id,
        url = url,
        method = method,
        requestHeaders = request_headers,
        requestBody = request_body,
        responseCode = response_code?.toInt(),
        responseHeaders = response_headers,
        responseBody = response_body,
        durationMs = duration_ms,
        timestamp = timestamp,
        isError = is_error != 0L
    )
}
