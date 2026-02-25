package io.netpeek.sdk

data class NetPeekConfig(
    val enabled: Boolean = true,
    val maxStoredCalls: Int = 500,
    val redactHeaders: List<String> = listOf("Authorization", "Cookie", "Set-Cookie")
)
