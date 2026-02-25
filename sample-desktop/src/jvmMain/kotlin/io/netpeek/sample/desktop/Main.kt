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

// Each button has its own identity so only the clicked one shows a spinner
private enum class RequestId { GET, POST, ERROR_404, TIMEOUT }

fun main() {
    NetPeek.init(DatabaseDriverFactory())

    val client = HttpClient(Java) {
        NetPeek.install(this, NetPeekConfig())
    }

    application {
        var inspectorOpen by remember { mutableStateOf(false) }
        val mainSnackbar = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        // Collect new-request events in the main window → show a snackbar toast
        // even when the inspector window is closed
        LaunchedEffect(Unit) {
            NetPeek.getRepository().newCallsFlow.collect { call ->
                val status = call.responseCode?.toString() ?: if (call.isError) "ERR" else "pending"
                mainSnackbar.showSnackbar(
                    message = "↗  ${call.method}  ${shortUrl(call.url)}  →  $status  (${call.durationMs}ms)",
                    duration = SnackbarDuration.Short
                )
            }
        }

        // ── Main app window ──
        Window(
            onCloseRequest = ::exitApplication,
            title = "My App",
            state = rememberWindowState(size = DpSize(820.dp, 580.dp))
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
                Scaffold(
                    snackbarHost = {
                        SnackbarHost(mainSnackbar) { data ->
                            Snackbar(
                                snackbarData = data,
                                containerColor = MaterialTheme.colorScheme.inverseSurface,
                                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                ) { padding ->
                    MyAppContent(
                        modifier = Modifier.padding(padding),
                        client = client,
                        onOpenInspector = { inspectorOpen = true }
                    )
                }
            }
        }

        // ── Inspector window ──
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
    onOpenInspector: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    // Per-button loading — only the active request's button shows a spinner
    var activeRequest by remember { mutableStateOf<RequestId?>(null) }
    var log by remember { mutableStateOf("← Fire a request to see it captured in the inspector") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text("My App", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "A sample app using the NetPeek SDK",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            FilledTonalButton(
                onClick = onOpenInspector,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Network Inspector")
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Text("API Endpoints", style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ApiButton(
                label = "GET  /get",
                color = Color(0xFF4CAF50),
                isLoading = activeRequest == RequestId.GET,
                enabled = activeRequest == null,
                modifier = Modifier.weight(1f)
            ) {
                activeRequest = RequestId.GET
                scope.launch {
                    log = runCatching {
                        client.get("https://httpbin.org/get")
                        "✓  GET https://httpbin.org/get  →  200 OK"
                    }.getOrElse { "✗  ${it.message}" }
                    activeRequest = null
                }
            }

            ApiButton(
                label = "POST  /post",
                color = Color(0xFF2196F3),
                isLoading = activeRequest == RequestId.POST,
                enabled = activeRequest == null,
                modifier = Modifier.weight(1f)
            ) {
                activeRequest = RequestId.POST
                scope.launch {
                    log = runCatching {
                        client.post("https://httpbin.org/post") {
                            contentType(ContentType.Application.Json)
                            setBody("""{"user":"alice","action":"login"}""")
                        }
                        "✓  POST https://httpbin.org/post  →  200 OK"
                    }.getOrElse { "✗  ${it.message}" }
                    activeRequest = null
                }
            }

            ApiButton(
                label = "404  /status/404",
                color = Color(0xFFF44336),
                isLoading = activeRequest == RequestId.ERROR_404,
                enabled = activeRequest == null,
                modifier = Modifier.weight(1f)
            ) {
                activeRequest = RequestId.ERROR_404
                scope.launch {
                    log = runCatching {
                        client.get("https://httpbin.org/status/404")
                        "✗  GET 404 Not Found (expected)"
                    }.getOrElse { "✗  ${it.message}" }
                    activeRequest = null
                }
            }

            ApiButton(
                label = "⏱  Timeout",
                color = Color(0xFFFF9800),
                isLoading = activeRequest == RequestId.TIMEOUT,
                enabled = activeRequest == null,
                modifier = Modifier.weight(1f)
            ) {
                activeRequest = RequestId.TIMEOUT
                scope.launch {
                    log = runCatching {
                        withTimeout(2000) { client.get("https://httpbin.org/delay/10") }
                        "OK"
                    }.getOrElse { "✗  Timed out after 2s (expected)" }
                    activeRequest = null
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
                if (activeRequest != null) {
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

        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "💡  Open Debug → Network Inspector from the menu bar, or click the button above.",
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
    isLoading: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
        } else {
            Text(label, fontFamily = FontFamily.Monospace)
        }
    }
}

private fun shortUrl(url: String): String {
    val path = url.removePrefix("https://").removePrefix("http://")
    return if (path.length > 40) path.take(37) + "…" else path
}
