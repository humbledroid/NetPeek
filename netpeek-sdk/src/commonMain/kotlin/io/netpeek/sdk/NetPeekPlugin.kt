package io.netpeek.sdk

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.bodyAsText
import io.ktor.http.contentType
import io.ktor.util.AttributeKey
import io.ktor.utils.io.charsets.Charsets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

class NetPeekPlugin internal constructor(
    private val config: NetPeekConfig,
    private val repository: NetworkCallRepository
) {

    companion object Plugin : HttpClientPlugin<NetPeekConfig, NetPeekPlugin> {
        override val key = AttributeKey<NetPeekPlugin>("NetPeek")

        private lateinit var sharedRepository: NetworkCallRepository

        fun init(repository: NetworkCallRepository) {
            sharedRepository = repository
        }

        override fun prepare(block: NetPeekConfig.() -> Unit): NetPeekPlugin {
            val cfg = NetPeekConfig().apply(block)
            return NetPeekPlugin(cfg, sharedRepository)
        }

        override fun install(plugin: NetPeekPlugin, scope: HttpClient) {
            if (!plugin.config.enabled) return

            scope.plugin(HttpSend).intercept { request ->
                val startTime = currentTimeMillis()
                val requestBody = runCatching { request.body.toString() }.getOrNull()
                val requestHeaders = request.headers.entries()
                    .associate { (k, v) ->
                        k to if (plugin.config.redactHeaders.any { it.equals(k, ignoreCase = true) }) "***" else v.joinToString(", ")
                    }.toJsonString()

                val result = runCatching { execute(request) }
                val durationMs = currentTimeMillis() - startTime

                val call = result.getOrNull()
                val isError = result.isFailure || (call?.response?.status?.value ?: 0) >= 400

                val responseCode = call?.response?.status?.value
                val responseBody = runCatching { call?.response?.bodyAsText() }.getOrNull()
                val responseHeaders = call?.response?.headers?.entries()
                    ?.associate { (k, v) ->
                        k to if (plugin.config.redactHeaders.any { it.equals(k, ignoreCase = true) }) "***" else v.joinToString(", ")
                    }?.toJsonString()

                val networkCall = NetworkCall(
                    url = request.url.buildString(),
                    method = request.method.value,
                    requestHeaders = requestHeaders,
                    requestBody = requestBody,
                    responseCode = responseCode,
                    responseHeaders = responseHeaders,
                    responseBody = responseBody,
                    durationMs = durationMs,
                    timestamp = startTime,
                    isError = isError
                )

                CoroutineScope(Dispatchers.IO).launch {
                    plugin.repository.insert(networkCall)
                }

                result.getOrThrow()
            }
        }
    }
}

private fun Map<String, String>.toJsonString(): String {
    val sb = StringBuilder("{")
    entries.forEachIndexed { i, (k, v) ->
        if (i > 0) sb.append(",")
        sb.append("\"").append(k.replace("\"", "\\\"")).append("\":\"").append(v.replace("\"", "\\\"")).append("\"")
    }
    sb.append("}")
    return sb.toString()
}

expect fun currentTimeMillis(): Long
