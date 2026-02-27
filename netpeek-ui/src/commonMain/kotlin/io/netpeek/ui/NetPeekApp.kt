package io.netpeek.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.netpeek.sdk.NetworkCall
import io.netpeek.sdk.NetworkCallRepository

/**
 * Root composable for the NetPeek inspector.
 *
 * @param repository    The SDK repository providing call data and the new-call flow.
 * @param initialCallId When launched from a notification tap, pass the call ID here to
 *                      navigate directly to that request's detail screen.
 *                      Null (default) opens the list.
 * @param onDismiss     Optional callback invoked when the user taps the close button.
 *                      When non-null, a close (×) button appears in the list screen's toolbar.
 *                      Use this when the inspector is presented modally.
 */
@Composable
fun NetPeekApp(
    repository: NetworkCallRepository,
    initialCallId: Long? = null,
    onDismiss: (() -> Unit)? = null
) {
    var selectedCall by remember { mutableStateOf<NetworkCall?>(null) }

    LaunchedEffect(initialCallId) {
        selectedCall = if (initialCallId != null && initialCallId > 0L) {
            repository.getCallById(initialCallId)
        } else {
            null
        }
    }

    if (selectedCall != null) {
        NetPeekDetailScreen(
            call   = selectedCall!!,
            onBack = { selectedCall = null }
        )
    } else {
        NetPeekListScreen(
            repository     = repository,
            onCallSelected = { selectedCall = it },
            onDismiss      = onDismiss
        )
    }
}
