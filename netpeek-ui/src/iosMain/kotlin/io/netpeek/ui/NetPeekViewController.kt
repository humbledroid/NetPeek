package io.netpeek.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.ComposeUIViewController
import io.netpeek.sdk.NetPeek
import platform.UIKit.UIViewController

fun createNetPeekViewController(): UIViewController = ComposeUIViewController {
    MaterialTheme {
        NetPeekApp(repository = NetPeek.getRepository())
    }
}
