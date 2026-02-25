package io.netpeek.sdk

data class NetPeekConfig(
    var enabled: Boolean = true,
    var maxStoredCalls: Int = 500,
    var redactHeaders: List<String> = listOf("Authorization", "Cookie", "Set-Cookie")
)
