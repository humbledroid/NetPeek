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
 *                      When this value changes (e.g. a second notification is tapped
 *                      while the inspector is already open), the UI navigates to the
 *                      newly requested call automatically.
 */
@Composable
fun NetPeekApp(
    repository: NetworkCallRepository,
    initialCallId: Long? = null
) {
    var selectedCall by remember { mutableStateOf<NetworkCall?>(null) }

    // Navigate to the specific call whenever initialCallId is set or changes.
    // This covers:
    //   1. Fresh launch from notification  → initialCallId arrives via onCreate
    //   2. Second tap while open           → initialCallId changes via onNewIntent
    //   3. No call ID (list launch)        → initialCallId is null, show list
    LaunchedEffect(initialCallId) {
        selectedCall = if (initialCallId != null && initialCallId > 0L) {
            repository.getCallById(initialCallId)  // null-safe: falls back to list if not found
        } else {
            null  // show the list
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
            onCallSelected = { selectedCall = it }
        )
    }
}
