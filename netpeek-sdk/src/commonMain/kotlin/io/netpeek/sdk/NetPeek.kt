package io.netpeek.sdk

import io.ktor.client.HttpClientConfig

object NetPeek {

    private var _repository: NetworkCallRepository? = null

    fun init(driverFactory: DatabaseDriverFactory, config: NetPeekConfig = NetPeekConfig()) {
        val repo = NetworkCallRepositoryImpl(driverFactory)
        _repository = repo
        NetPeekPlugin.init(repo)
    }

    fun install(clientConfig: HttpClientConfig<*>, config: NetPeekConfig = NetPeekConfig()) {
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
