package io.netpeek.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
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
 * Full notification system for NetPeek on Android.
 *
 * Provides two notification styles:
 *  1. Persistent summary — always-visible notification showing live request
 *     count + last request. Tap → opens NetPeekActivity. Has "Clear All" action.
 *  2. Per-request notifications — one notification per captured call, grouped
 *     under the summary so the drawer stays tidy.
 *
 * Usage:
 *   NetPeekNotifier.start(context)   // in Application.onCreate or Activity.onCreate
 *   NetPeekNotifier.stop()           // in onDestroy
 */
object NetPeekNotifier {

    // ── Channel IDs ──────────────────────────────────────────────────────────
    private const val CHANNEL_SUMMARY  = "netpeek_summary"
    private const val CHANNEL_REQUESTS = "netpeek_requests"

    // ── Notification IDs ─────────────────────────────────────────────────────
    private const val SUMMARY_ID       = 9000   // persistent, always updated
    private const val GROUP_KEY        = "io.netpeek.REQUESTS"
    private val requestNotifId         = AtomicInteger(9100)

    private var job: Job? = null
    private var requestCount           = AtomicInteger(0)
    private var lastCallSummary        = ""

    // ── Public API ───────────────────────────────────────────────────────────

    fun start(context: Context) {
        val appCtx = context.applicationContext
        createChannels(appCtx)
        showSummaryNotification(appCtx)          // show immediately (0 requests)

        job = CoroutineScope(Dispatchers.Default).launch {
            NetPeek.getRepository().newCallsFlow.collect { call ->
                if (!hasPermission(appCtx)) return@collect
                val count = requestCount.incrementAndGet()
                lastCallSummary = buildSummaryLine(call)
                showSummaryNotification(appCtx, count, lastCallSummary)
                showRequestNotification(appCtx, call)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    /** Call this to dismiss everything when the user clears all via the SDK. */
    fun dismissAll(context: Context) {
        requestCount.set(0)
        lastCallSummary = ""
        NotificationManagerCompat.from(context).cancelAll()
        showSummaryNotification(context.applicationContext)
    }

    // ── Channels ─────────────────────────────────────────────────────────────

    private fun createChannels(context: Context) {
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Summary channel — silent, persistent, low-priority
        if (mgr.getNotificationChannel(CHANNEL_SUMMARY) == null) {
            mgr.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_SUMMARY,
                    "NetPeek — Live Monitor",
                    NotificationManager.IMPORTANCE_MIN
                ).apply {
                    description = "Persistent notification showing captured request count"
                    setShowBadge(false)
                    enableLights(false)
                    enableVibration(false)
                }
            )
        }

        // Per-request channel — default importance, lights + vibrate for errors
        if (mgr.getNotificationChannel(CHANNEL_REQUESTS) == null) {
            mgr.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_REQUESTS,
                    "NetPeek — Requests",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "One notification per captured network request"
                    setShowBadge(true)
                    lightColor = Color.CYAN
                    enableLights(true)
                }
            )
        }
    }

    // ── Summary notification ─────────────────────────────────────────────────

    private fun showSummaryNotification(
        context: Context,
        count: Int = 0,
        lastLine: String = "Waiting for requests…"
    ) {
        val openIntent = pendingInspectorIntent(context, requestCode = 1)

        val clearIntent = PendingIntent.getBroadcast(
            context,
            2,
            Intent(context, NetPeekClearReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (count == 0) "🔍 NetPeek — waiting…"
                    else "🔍 NetPeek — $count request${if (count == 1) "" else "s"} captured"

        val notif = NotificationCompat.Builder(context, CHANNEL_SUMMARY)
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setContentTitle(title)
            .setContentText(lastLine)
            .setStyle(NotificationCompat.BigTextStyle().bigText(lastLine))
            .setContentIntent(openIntent)
            .setOngoing(true)           // can't be swiped away
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .addAction(
                android.R.drawable.ic_menu_view,
                "Open Inspector",
                openIntent
            )
            .addAction(
                android.R.drawable.ic_menu_delete,
                "Clear All",
                clearIntent
            )
            .build()

        NotificationManagerCompat.from(context).notify(SUMMARY_ID, notif)
    }

    // ── Per-request notification ─────────────────────────────────────────────

    private fun showRequestNotification(context: Context, call: NetworkCall) {
        val status   = call.responseCode?.toString() ?: if (call.isError) "ERR" else "…"
        val duration = call.durationMs?.let { " (${it}ms)" } ?: ""
        val title    = "${statusEmoji(call)}  ${call.method}  →  $status$duration"
        val url      = call.url

        val openIntent = pendingInspectorIntent(context, requestCode = requestNotifId.get())

        val color = when {
            call.isError                          -> Color.RED
            (call.responseCode ?: 0) >= 400       -> 0xFFFF9800.toInt()
            else                                  -> 0xFF4CAF50.toInt()
        }

        val notif = NotificationCompat.Builder(context, CHANNEL_REQUESTS)
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setColor(color)
            .setColorized(true)
            .setContentTitle(title)
            .setContentText(url)
            .setStyle(NotificationCompat.BigTextStyle().bigText(url))
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setGroup(GROUP_KEY)
            .build()

        NotificationManagerCompat.from(context)
            .notify(requestNotifId.incrementAndGet(), notif)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun pendingInspectorIntent(context: Context, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            NetPeekActivity.newIntent(context).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun buildSummaryLine(call: NetworkCall): String {
        val status = call.responseCode?.toString() ?: if (call.isError) "ERR" else "…"
        val url    = call.url.removePrefix("https://").removePrefix("http://")
            .let { if (it.length > 55) it.take(52) + "…" else it }
        return "${statusEmoji(call)}  ${call.method}  $url  →  $status"
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
