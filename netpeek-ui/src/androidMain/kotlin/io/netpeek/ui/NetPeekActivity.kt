package io.netpeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
                // FLAG_ACTIVITY_NEW_TASK — required when starting from non-Activity context
                // (notifications, BroadcastReceiver).
                // No FLAG_ACTIVITY_MULTIPLE_TASK — that bypasses singleTask and prevents
                // onNewIntent from firing, which broke the deep-link navigation.
                // The custom taskAffinity in the manifest is enough to keep the inspector
                // as a separate recents entry.
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
    }

    // Reactive state — Compose observes this and recomposes when a new intent arrives.
    private var callIdState by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        callIdState = extractCallId(intent)

        setContent {
            MaterialTheme {
                NetPeekApp(
                    repository    = NetPeek.getRepository(),
                    initialCallId = callIdState
                )
            }
        }
    }

    /**
     * Called when the activity is already running (singleTask) and a new intent arrives —
     * e.g. the user taps a different request's notification while the inspector is open.
     * Updating [callIdState] triggers recomposition without recreating the activity.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        callIdState = extractCallId(intent)
    }

    private fun extractCallId(i: Intent): Long? =
        i.takeIf { it.hasExtra(EXTRA_CALL_ID) }
         ?.getLongExtra(EXTRA_CALL_ID, -1L)
         ?.takeIf { it > 0L }
}
