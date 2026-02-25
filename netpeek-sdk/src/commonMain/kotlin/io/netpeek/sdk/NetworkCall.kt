package io.netpeek.sdk

data class NetworkCall(
    val id: Long = 0L,
    val url: String,
    val method: String,
    val requestHeaders: String,
    val requestBody: String? = null,
    val responseCode: Int? = null,
    val responseHeaders: String? = null,
    val responseBody: String? = null,
    val durationMs: Long? = null,
    val timestamp: Long,
    val isError: Boolean = false
)
