package io.netpeek.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.netpeek.sdk.NetPeek
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles the "Clear All" action button tapped from the NetPeek persistent notification.
 * Registered in the host app's AndroidManifest.xml via netpeek-ui's manifest merge.
 */
class NetPeekClearReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        CoroutineScope(Dispatchers.Default).launch {
            NetPeek.getRepository().clearAll()
        }
        NetPeekNotifier.dismissAll(context)
    }
}
