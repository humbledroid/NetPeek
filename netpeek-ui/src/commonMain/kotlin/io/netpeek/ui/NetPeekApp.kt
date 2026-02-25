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
 * @param repository  The SDK repository providing call data.
 * @param initialCallId  When launched from a notification, pass the call ID here
 *                       to navigate directly to that request's detail screen.
 *                       Pass null (default) to open the list.
 */
@Composable
fun NetPeekApp(
    repository: NetworkCallRepository,
    initialCallId: Long? = null
) {
    var selectedCall by remember { mutableStateOf<NetworkCall?>(null) }

    // If launched from a notification with a specific call ID,
    // look up that call and jump straight to its detail screen.
    LaunchedEffect(initialCallId) {
        if (initialCallId != null && initialCallId > 0L) {
            selectedCall = repository.getCallById(initialCallId)
        }
    }

    if (selectedCall != null) {
        NetPeekDetailScreen(
            call = selectedCall!!,
            onBack = { selectedCall = null }
        )
    } else {
        NetPeekListScreen(
            repository = repository,
            onCallSelected = { selectedCall = it }
        )
    }
}
