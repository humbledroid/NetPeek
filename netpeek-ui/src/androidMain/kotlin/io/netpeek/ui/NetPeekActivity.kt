package io.netpeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import io.netpeek.sdk.NetPeek

class NetPeekActivity : ComponentActivity() {

    companion object {
        fun newIntent(context: Context): Intent =
            Intent(context, NetPeekActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                NetPeekApp(repository = NetPeek.getRepository())
            }
        }
    }
}
