package io.netpeek.ui

import io.netpeek.sdk.NetPeek
import io.netpeek.sdk.NetworkCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import platform.Foundation.NSNumber
import platform.UIKit.UIApplication
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationAction
import platform.UserNotifications.UNNotificationActionOptionDestructive
import platform.UserNotifications.UNNotificationActionOptionForeground
import platform.UserNotifications.UNNotificationCategory
import platform.UserNotifications.UNNotificationCategoryOptionNone
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

/**
 * iOS notification system for NetPeek using UNUserNotificationCenter.
 *
 * Features:
 *  - Per-request local notifications with method, status, duration
 *  - App icon badge increments per request, resets when app becomes active
 *  - Notification actions: "Open Inspector" (foreground) + "Dismiss" (destructive)
 *  - Error/4xx requests trigger a different sound from 2xx
 *
 * Swift integration:
 *   // In AppDelegate / @main App:
 *   NetPeekNotifier.shared.requestPermission(provisional: false)
 *   NetPeekNotifier.shared.start()
 *
 *   // Reset badge when app becomes active:
 *   NetPeekNotifier.shared.resetBadge()
 *
 *   // Handle notification actions in UNUserNotificationCenterDelegate:
 *   NetPeekViewControllerKt.handleNotificationResponse(response: response)
 */
object NetPeekNotifier {

    private const val CATEGORY_ID    = "NETPEEK_REQUEST"
    const val ACTION_OPEN            = "NETPEEK_OPEN"
    const val ACTION_DISMISS         = "NETPEEK_DISMISS"

    private var job: Job? = null
    private var badgeCount = 0

    // ── Public API ───────────────────────────────────────────────────────────

    fun requestPermission(provisional: Boolean = false) {
        val options = UNAuthorizationOptionAlert or
                      UNAuthorizationOptionSound or
                      UNAuthorizationOptionBadge
        UNUserNotificationCenter.currentNotificationCenter()
            .requestAuthorizationWithOptions(options) { granted, _ ->
                if (granted) registerCategories()
            }
    }

    fun start() {
        registerCategories()
        job = CoroutineScope(Dispatchers.Default).launch {
            NetPeek.getRepository().newCallsFlow.collect { call ->
                postNotification(call)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun resetBadge() {
        badgeCount = 0
        UIApplication.sharedApplication.applicationIconBadgeNumber = 0
        UNUserNotificationCenter.currentNotificationCenter()
            .removeAllDeliveredNotifications()
    }

    // ── Notification posting ─────────────────────────────────────────────────

    private fun postNotification(call: NetworkCall) {
        badgeCount++

        val status   = call.responseCode?.toString() ?: if (call.isError) "ERR" else "…"
        val duration = call.durationMs?.let { " · ${it}ms" } ?: ""
        val emoji    = statusEmoji(call)

        val content = UNMutableNotificationContent()
        content.setTitle("$emoji  ${call.method}  →  $status$duration")
        content.setBody(call.url
            .removePrefix("https://")
            .removePrefix("http://")
            .let { if (it.length > 80) it.take(77) + "…" else it })
        content.setSound(if (call.isError || (call.responseCode ?: 0) >= 400)
            UNNotificationSound.defaultSound()    // critical sound requires entitlement
            else UNNotificationSound.defaultSound())
        content.setBadge(NSNumber(int = badgeCount))
        content.setCategoryIdentifier(CATEGORY_ID)
        content.setUserInfo(mapOf(
            "netpeek_call_id" to call.id.toString(), // stored as String to avoid NSNumber boxing
            "netpeek_url"     to call.url,
            "netpeek_method"  to call.method,
            "netpeek_status"  to status
        ))

        // Trigger immediately (minimum interval: 0.1s)
        val trigger = UNTimeIntervalNotificationTrigger
            .triggerWithTimeInterval(0.1, repeats = false)

        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = "netpeek_${call.timestamp}_${call.method}",
            content    = content,
            trigger    = trigger
        )

        UNUserNotificationCenter.currentNotificationCenter()
            .addNotificationRequest(request) { /* ignore add error */ }
    }

    // ── Categories & Actions ─────────────────────────────────────────────────

    private fun registerCategories() {
        val openAction = UNNotificationAction.actionWithIdentifier(
            identifier = ACTION_OPEN,
            title      = "Open Inspector",
            options    = UNNotificationActionOptionForeground
        )

        val dismissAction = UNNotificationAction.actionWithIdentifier(
            identifier = ACTION_DISMISS,
            title      = "Dismiss",
            options    = UNNotificationActionOptionDestructive
        )

        val category = UNNotificationCategory.categoryWithIdentifier(
            identifier        = CATEGORY_ID,
            actions           = listOf(openAction, dismissAction),
            intentIdentifiers = emptyList<Any>(),
            options           = UNNotificationCategoryOptionNone
        )

        UNUserNotificationCenter.currentNotificationCenter()
            .setNotificationCategories(setOf(category))
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun statusEmoji(call: NetworkCall) = when {
        call.isError                         -> "🔴"
        (call.responseCode ?: 0) >= 400      -> "🟠"
        (call.responseCode ?: 0) >= 200      -> "🟢"
        else                                 -> "⚪"
    }
}

/**
 * Handle NetPeek notification actions in your UNUserNotificationCenterDelegate.
 * Returns true if the response belonged to NetPeek (so you know not to handle it).
 */
fun handleNotificationResponse(
    response: platform.UserNotifications.UNNotificationResponse
): Boolean {
    val category = response.notification.request.content.categoryIdentifier
    if (category != "NETPEEK_REQUEST") return false
    // ACTION_OPEN is handled in Swift side (present the inspector VC)
    return true
}
