package io.netpeek.sample.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.netpeek.sample.SampleScreen
import io.netpeek.sample.SampleViewModel
import io.netpeek.sdk.DatabaseDriverFactory
import io.netpeek.sdk.NetPeek
import io.netpeek.sdk.NetPeekConfig
import io.netpeek.ui.NetPeekApp

fun main() {
    NetPeek.init(DatabaseDriverFactory())

    val client = HttpClient(Java) {
        NetPeek.install(this, NetPeekConfig())
    }
    val viewModel = SampleViewModel(client)

    application {
        var inspectorOpen by remember { mutableStateOf(false) }

        // ── Main app window ──
        Window(
            onCloseRequest = { viewModel.close(); exitApplication() },
            title = "NetPeek Sample",
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
                SampleScreen(
                    viewModel = viewModel,
                    onOpenInspector = { inspectorOpen = true }
                )
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
