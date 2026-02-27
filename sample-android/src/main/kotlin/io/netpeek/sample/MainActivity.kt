package io.netpeek.sample

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.netpeek.sdk.DatabaseDriverFactory
import io.netpeek.sdk.NetPeek
import io.netpeek.sdk.NetPeekConfig
import io.netpeek.ui.NetPeekActivity
import io.netpeek.ui.NetPeekNotifier

class MainActivity : ComponentActivity() {

    private val client by lazy {
        HttpClient(Android) { NetPeek.install(this, NetPeekConfig()) }
    }

    private val viewModel by lazy { SampleViewModel(client) }

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
            MaterialTheme {
                SampleScreen(
                    viewModel = viewModel,
                    onOpenInspector = { startActivity(NetPeekActivity.newIntent(this)) }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        NetPeekNotifier.stop()
        viewModel.close()
    }
}
