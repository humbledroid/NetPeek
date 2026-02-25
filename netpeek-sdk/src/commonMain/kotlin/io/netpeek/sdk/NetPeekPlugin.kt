package io.netpeek.sdk

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.util.AttributeKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

                val requestHeaders = request.headers.entries()
                    .associate { (k, v) ->
                        k to if (plugin.config.redactHeaders.any { it.equals(k, ignoreCase = true) }) "***"
                              else v.joinToString(", ")
                    }.toJsonString()

                val result = runCatching { execute(request) }
                val durationMs = currentTimeMillis() - startTime

                val httpCall = result.getOrNull()
                val responseCode = httpCall?.response?.status?.value
                val isError = result.isFailure || (responseCode ?: 0) >= 400

                val responseHeaders = httpCall?.response?.headers?.entries()
                    ?.associate { (k, v) ->
                        k to if (plugin.config.redactHeaders.any { it.equals(k, ignoreCase = true) }) "***"
                              else v.joinToString(", ")
                    }?.toJsonString()

                val networkCall = NetworkCall(
                    url = request.url.buildString(),
                    method = request.method.value,
                    requestHeaders = requestHeaders,
                    requestBody = null, // body capture deferred — Ktor 3.x body is OutgoingContent
                    responseCode = responseCode,
                    responseHeaders = responseHeaders,
                    responseBody = null, // body capture avoids double-read; use logging plugin for body
                    durationMs = durationMs,
                    timestamp = startTime,
                    isError = isError
                )

                CoroutineScope(Dispatchers.Default).launch {
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
        sb.append("\"").append(k.replace("\"", "\\\""))
            .append("\":\"").append(v.replace("\"", "\\\"")).append("\"")
    }
    sb.append("}")
    return sb.toString()
}

expect fun currentTimeMillis(): Long
