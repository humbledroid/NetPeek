package io.netpeek.sample

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

enum class ActiveRequest { GET, POST, ERROR_404, TIMEOUT }

data class SampleUiState(
    val log: String = "Tap a button to fire a request",
    val activeRequest: ActiveRequest? = null
)

/**
 * Shared ViewModel for the NetPeek sample app.
 * Receives a pre-configured HttpClient (with NetPeek installed) from the platform entry point.
 */
class SampleViewModel(private val client: HttpClient) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _state = MutableStateFlow(SampleUiState())
    val state: StateFlow<SampleUiState> = _state

    fun fireGet() = fire(ActiveRequest.GET) {
        client.get("https://httpbin.org/get")
        "✓  GET https://httpbin.org/get  →  200 OK"
    }

    fun firePost() = fire(ActiveRequest.POST) {
        client.post("https://httpbin.org/post") {
            contentType(ContentType.Application.Json)
            setBody("""{"user":"alice","action":"login"}""")
        }
        "✓  POST https://httpbin.org/post  →  200 OK"
    }

    fun fire404() = fire(ActiveRequest.ERROR_404) {
        client.get("https://httpbin.org/status/404")
        "✗  GET 404 Not Found (expected)"
    }

    fun fireTimeout() = fire(ActiveRequest.TIMEOUT) {
        withTimeout(2000) { client.get("https://httpbin.org/delay/10") }
        "OK"
    }

    private fun fire(id: ActiveRequest, block: suspend () -> String) {
        if (_state.value.activeRequest != null) return
        _state.value = _state.value.copy(activeRequest = id)
        scope.launch {
            val result = runCatching { block() }.getOrElse { "✗  ${it.message}" }
            _state.value = _state.value.copy(log = result, activeRequest = null)
        }
    }

    fun close() {
        scope.cancel()
        client.close()
    }
}
