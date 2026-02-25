package io.netpeek.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.netpeek.sdk.NetworkCall
import io.netpeek.sdk.NetworkCallRepository

@Composable
fun NetPeekApp(repository: NetworkCallRepository) {
    var selectedCall by remember { mutableStateOf<NetworkCall?>(null) }

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
