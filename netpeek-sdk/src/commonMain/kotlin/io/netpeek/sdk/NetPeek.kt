package io.netpeek.sdk

import io.ktor.client.HttpClientConfig

object NetPeek {

    private var _repository: NetworkCallRepository? = null

    // Explicit no-config overloads for Swift/ObjC callers:
    // Kotlin default parameters don't generate separate ObjC bridge overloads.
    fun init(driverFactory: DatabaseDriverFactory) = init(driverFactory, NetPeekConfig())

    fun init(driverFactory: DatabaseDriverFactory, config: NetPeekConfig) {
        val repo = NetworkCallRepositoryImpl(driverFactory)
        _repository = repo
        NetPeekPlugin.init(repo)
    }

    fun install(clientConfig: HttpClientConfig<*>) = install(clientConfig, NetPeekConfig())

    fun install(clientConfig: HttpClientConfig<*>, config: NetPeekConfig) {
        clientConfig.install(NetPeekPlugin) {
            enabled = config.enabled
            maxStoredCalls = config.maxStoredCalls
            redactHeaders = config.redactHeaders
        }
    }

    fun getRepository(): NetworkCallRepository {
        return _repository ?: error("NetPeek not initialized. Call NetPeek.init() first.")
    }
}
