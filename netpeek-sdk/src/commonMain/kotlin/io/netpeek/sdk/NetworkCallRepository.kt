package io.netpeek.sdk

import kotlinx.coroutines.flow.Flow

interface NetworkCallRepository {
    fun getAllCalls(): Flow<List<NetworkCall>>
    suspend fun getCallById(id: Long): NetworkCall?
    suspend fun insert(call: NetworkCall)
    suspend fun clearAll()
    suspend fun count(): Long
}
