package io.netpeek.sample

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.netpeek.sdk.DatabaseDriverFactory
import io.netpeek.sdk.NetPeek
import io.netpeek.sdk.NetPeekConfig
import io.netpeek.ui.NetPeekActivity
import io.netpeek.ui.NetPeekNotifier
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class MainActivity : ComponentActivity() {

    private val client by lazy {
        HttpClient { NetPeek.install(this, NetPeekConfig()) }
    }

    // Permission launcher for POST_NOTIFICATIONS (Android 13+)
    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // granted or denied — NetPeekNotifier checks permission before posting
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NetPeek.init(DatabaseDriverFactory(this))

        // Start notification listener — fires a system notification per request
        NetPeekNotifier.start(this)

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val scope = rememberCoroutineScope()
            MaterialTheme {
                Scaffold(
                    floatingActionButton = {
                        ExtendedFloatingActionButton(
                            text = { Text("Inspect") },
                            icon = { Icon(Icons.Default.List, null) },
                            onClick = { startActivity(NetPeekActivity.newIntent(this@MainActivity)) }
                        )
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("NetPeek Sample", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Requests fire a system notification — tap to open the inspector.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(32.dp))

                        Button(
                            onClick = { scope.launch { runCatching { client.get("https://httpbin.org/get") } } },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("GET Request") }

                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    runCatching {
                                        client.post("https://httpbin.org/post") {
                                            contentType(ContentType.Application.Json)
                                            setBody("""{"hello":"world"}""")
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("POST Request") }

                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { scope.launch { runCatching { client.get("https://httpbin.org/status/404") } } },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("404 Error") }

                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    runCatching { withTimeout(2000) { client.get("https://httpbin.org/delay/10") } }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Timeout") }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        NetPeekNotifier.stop()
    }
}
