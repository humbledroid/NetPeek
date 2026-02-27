package io.netpeek.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.netpeek.sdk.NetPeek
import io.netpeek.ui.NetPeekApp
import platform.UIKit.UIViewController

/**
 * Creates a UIViewController hosting the shared KMP sample screen.
 *
 * On multi-scene capable devices (all modern iPhones/iPads with iOS 16+),
 * tapping the inspector button invokes [onOpenInspector], which the Swift
 * caller should implement using SwiftUI's `openWindow(id:)` to open the
 * inspector as a **separate scene** visible in the iOS app switcher —
 * mirroring the separate-task behaviour on Android.
 *
 * If no callback is provided, the inspector falls back to displaying
 * inline within the same view controller.
 *
 * Usage (Swift):
 *   // With multi-window support via SwiftUI openWindow:
 *   SampleViewControllerKt.createSampleViewController(onOpenInspector: {
 *       openWindow(id: "netpeek-inspector")
 *   })
 *
 *   // Inline fallback (no multi-window):
 *   SampleViewControllerKt.createSampleViewController()
 */
fun createSampleViewController(): UIViewController =
    buildSampleViewController(null)

fun createSampleViewController(onOpenInspector: () -> Unit): UIViewController =
    buildSampleViewController(onOpenInspector)

private fun buildSampleViewController(onOpenInspector: (() -> Unit)?): UIViewController {
    val client = HttpClient(Darwin) { NetPeek.install(this) }
    val vm = SampleViewModel(client)
    return ComposeUIViewController {
        var showInspectorInline by remember { mutableStateOf(false) }
        DisposableEffect(Unit) { onDispose { vm.close() } }
        MaterialTheme {
            if (showInspectorInline) {
                Box(Modifier.fillMaxSize()) {
                    NetPeekApp(
                        repository = NetPeek.getRepository(),
                        onDismiss  = { showInspectorInline = false }
                    )
                }
            } else {
                SampleScreen(vm, onOpenInspector = {
                    if (onOpenInspector != null) {
                        onOpenInspector()
                    } else {
                        showInspectorInline = true
                    }
                })
            }
        }
    }
}
