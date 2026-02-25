package io.netpeek.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import io.netpeek.sdk.NetPeek

class NetPeekActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                NetPeekApp(repository = NetPeek.getRepository())
            }
        }
    }
}
