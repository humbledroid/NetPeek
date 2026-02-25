package io.netpeek.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.singleWindowApplication
import io.netpeek.sdk.NetPeek

fun launchNetPeekWindow() = singleWindowApplication(title = "NetPeek") {
    MaterialTheme {
        NetPeekApp(repository = NetPeek.getRepository())
    }
}
