package io.netpeek.sdk

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface NetworkCallRepository {
    /** Emits every time a new call is captured — use this to drive notifications. */
    val newCallsFlow: SharedFlow<NetworkCall>

    fun getAllCalls(): Flow<List<NetworkCall>>
    suspend fun getCallById(id: Long): NetworkCall?
    suspend fun insert(call: NetworkCall)
    suspend fun clearAll()
    suspend fun count(): Long
}
