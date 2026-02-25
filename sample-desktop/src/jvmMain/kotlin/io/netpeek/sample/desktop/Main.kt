package io.netpeek.sample.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.netpeek.sdk.DatabaseDriverFactory
import io.netpeek.sdk.NetPeek
import io.netpeek.sdk.NetPeekConfig
import io.netpeek.ui.NetPeekApp
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

fun main() {
    NetPeek.init(DatabaseDriverFactory())

    val client = HttpClient(Java) {
        NetPeek.install(this, NetPeekConfig())
    }

    application {
        // Inspector window state — hidden by default
        var inspectorOpen by remember { mutableStateOf(false) }

        // ── Main app window ──
        Window(
            onCloseRequest = ::exitApplication,
            title = "My App",
            state = rememberWindowState(size = DpSize(800.dp, 560.dp))
        ) {
            MenuBar {
                Menu("Debug") {
                    Item(
                        text = if (inspectorOpen) "Close Network Inspector" else "Network Inspector",
                        onClick = { inspectorOpen = !inspectorOpen }
                    )
                }
            }

            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MyAppContent(
                        client = client,
                        onOpenInspector = { inspectorOpen = true }
                    )
                }
            }
        }

        // ── Inspector window — only shown when triggered ──
        if (inspectorOpen) {
            Window(
                onCloseRequest = { inspectorOpen = false },
                title = "NetPeek — Network Inspector",
                state = rememberWindowState(size = DpSize(860.dp, 650.dp))
            ) {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    NetPeekApp(repository = NetPeek.getRepository())
                }
            }
        }
    }
}

@Composable
private fun MyAppContent(
    client: HttpClient,
    onOpenInspector: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var log by remember { mutableStateOf("← Fire a request to see it captured in the inspector") }
    var loading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // App header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    "My App",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "A sample app using the NetPeek SDK",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Debug button — how the SDK is surfaced to the developer
            FilledTonalButton(
                onClick = onOpenInspector,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Icon(
                    Icons.Default.BugReport,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Network Inspector")
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Fake app content area
        Text(
            "API Endpoints",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ApiButton(
                label = "GET  /get",
                color = Color(0xFF4CAF50),
                enabled = !loading
            ) {
                scope.launch {
                    loading = true
                    log = runCatching {
                        client.get("https://httpbin.org/get")
                        "✓ GET https://httpbin.org/get → 200 OK"
                    }.getOrElse { "✗ ${it.message}" }
                    loading = false
                }
            }

            ApiButton(
                label = "POST  /post",
                color = Color(0xFF2196F3),
                enabled = !loading
            ) {
                scope.launch {
                    loading = true
                    log = runCatching {
                        client.post("https://httpbin.org/post") {
                            contentType(ContentType.Application.Json)
                            setBody("""{"user":"alice","action":"login"}""")
                        }
                        "✓ POST https://httpbin.org/post → 200 OK"
                    }.getOrElse { "✗ ${it.message}" }
                    loading = false
                }
            }

            ApiButton(
                label = "404  /status/404",
                color = Color(0xFFF44336),
                enabled = !loading
            ) {
                scope.launch {
                    loading = true
                    log = runCatching {
                        client.get("https://httpbin.org/status/404")
                        "✓ GET 404"
                    }.getOrElse { "✗ 404 Not Found (expected)" }
                    loading = false
                }
            }

            ApiButton(
                label = "⏱  Timeout",
                color = Color(0xFFFF9800),
                enabled = !loading
            ) {
                scope.launch {
                    loading = true
                    log = runCatching {
                        withTimeout(2000) { client.get("https://httpbin.org/delay/10") }
                        "OK"
                    }.getOrElse { "✗ Request timed out after 2s (expected)" }
                    loading = false
                }
            }
        }

        // Response log
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                }
                Text(
                    text = log,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Hint
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "💡  Open Debug → Network Inspector from the menu bar, or click the button above to inspect captured traffic.",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ApiButton(
    label: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(label, fontFamily = FontFamily.Monospace)
    }
}
