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
        const val EXTRA_CALL_ID = "netpeek_call_id"

        /** Open the inspector list. */
        fun newIntent(context: Context): Intent = buildIntent(context)

        /** Open the inspector and jump directly to the given request's detail. */
        fun newIntent(context: Context, callId: Long): Intent =
            buildIntent(context).putExtra(EXTRA_CALL_ID, callId)

        private fun buildIntent(context: Context) =
            Intent(context, NetPeekActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callId = intent
            .takeIf { it.hasExtra(EXTRA_CALL_ID) }
            ?.getLongExtra(EXTRA_CALL_ID, -1L)
            ?.takeIf { it > 0L }

        setContent {
            MaterialTheme {
                NetPeekApp(
                    repository    = NetPeek.getRepository(),
                    initialCallId = callId
                )
            }
        }
    }

    // Re-read the call ID if the activity is re-launched via FLAG_ACTIVITY_SINGLE_TOP
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()   // simplest way to pick up the new initialCallId
    }
}
