package io.netpeek.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.netpeek.sdk.NetPeek
import io.netpeek.sdk.NetworkCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

/**
 * Listens to [NetworkCallRepository.newCallsFlow] and posts a system notification
 * for every captured request. Call [start] once (e.g. in Application.onCreate or
 * MainActivity.onCreate). Call [stop] when the app exits.
 */
object NetPeekNotifier {

    private const val CHANNEL_ID   = "netpeek_requests"
    private const val CHANNEL_NAME = "NetPeek — Network Requests"
    private val notifId = AtomicInteger(1000)
    private var job: Job? = null

    fun start(context: Context) {
        createChannel(context)
        val appCtx = context.applicationContext
        job = CoroutineScope(Dispatchers.Default).launch {
            NetPeek.getRepository().newCallsFlow.collect { call ->
                if (hasPermission(appCtx)) postNotification(appCtx, call)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private fun createChannel(context: Context) {
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
            mgr.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW).apply {
                    description = "One notification per captured network request"
                    setShowBadge(false)
                }
            )
        }
    }

    private fun postNotification(context: Context, call: NetworkCall) {
        val status  = call.responseCode?.toString() ?: if (call.isError) "ERR" else "pending"
        val duration = call.durationMs?.let { "${it}ms" } ?: ""
        val title   = "${statusEmoji(call)}  ${call.method}  →  $status  $duration".trim()
        val text    = call.url

        val tapIntent = PendingIntent.getActivity(
            context, 0,
            NetPeekActivity.newIntent(context).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        NotificationManagerCompat.from(context).notify(notifId.incrementAndGet(), notification)
    }

    private fun statusEmoji(call: NetworkCall) = when {
        call.isError                         -> "🔴"
        (call.responseCode ?: 0) >= 400      -> "🟠"
        (call.responseCode ?: 0) >= 200      -> "🟢"
        else                                 -> "⚪"
    }

    private fun hasPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}
