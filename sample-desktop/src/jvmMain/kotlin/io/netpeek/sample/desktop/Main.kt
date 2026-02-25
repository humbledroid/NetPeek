package io.netpeek.sample.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.netpeek.sdk.DatabaseDriverFactory
import io.netpeek.sdk.NetPeek
import io.netpeek.sdk.NetPeekConfig
import io.netpeek.ui.NetPeekApp
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

fun main() {
    // Init NetPeek with JVM driver
    NetPeek.init(DatabaseDriverFactory())

    val client = HttpClient(Java) {
        NetPeek.install(this, NetPeekConfig())
    }

    application {
        val windowState = rememberWindowState(width = 1200.dp, height = 750.dp)

        Window(
            onCloseRequest = ::exitApplication,
            title = "NetPeek Desktop",
            state = windowState
        ) {
            MaterialTheme(
                colorScheme = darkColorScheme()
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.fillMaxSize()) {

                        // ── Left panel: test controls ──
                        SamplePanel(
                            client = client,
                            modifier = Modifier.width(300.dp).fillMaxHeight()
                        )

                        VerticalDivider(
                            modifier = Modifier.fillMaxHeight(),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        // ── Right panel: live inspector ──
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            NetPeekApp(repository = NetPeek.getRepository())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SamplePanel(client: HttpClient, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var lastResult by remember { mutableStateOf("Fire a request →") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "NetPeek",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Fire requests and watch them appear in the inspector →",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(Modifier.height(8.dp))

        RequestButton(
            label = "GET  /get",
            color = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            loading = isLoading
        ) {
            scope.launch {
                isLoading = true
                lastResult = runCatching {
                    client.get("https://httpbin.org/get")
                    "GET 200 OK"
                }.getOrElse { "Error: ${it.message}" }
                isLoading = false
            }
        }

        RequestButton(
            label = "POST  /post  { JSON }",
            color = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            loading = isLoading
        ) {
            scope.launch {
                isLoading = true
                lastResult = runCatching {
                    client.post("https://httpbin.org/post") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"hello":"world","sdk":"netpeek"}""")
                    }
                    "POST 200 OK"
                }.getOrElse { "Error: ${it.message}" }
                isLoading = false
            }
        }

        RequestButton(
            label = "GET  /status/404",
            color = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            loading = isLoading
        ) {
            scope.launch {
                isLoading = true
                lastResult = runCatching {
                    client.get("https://httpbin.org/status/404")
                    "404"
                }.getOrElse { "404 (expected)" }
                isLoading = false
            }
        }

        RequestButton(
            label = "GET  /delay/10  (timeout)",
            color = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary
            ),
            loading = isLoading
        ) {
            scope.launch {
                isLoading = true
                lastResult = runCatching {
                    withTimeout(2000) { client.get("https://httpbin.org/delay/10") }
                    "OK"
                }.getOrElse { "Timed out (expected)" }
                isLoading = false
            }
        }

        Spacer(Modifier.weight(1f))

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = lastResult,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RequestButton(
    label: String,
    color: ButtonColors,
    loading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !loading,
        colors = color,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(label)
        }
    }
}
