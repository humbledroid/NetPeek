package io.netpeek.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.ComposeUIViewController
import io.netpeek.sdk.NetPeek
import platform.UIKit.UIViewController

/** Opens the inspector list. */
fun createNetPeekViewController(): UIViewController = ComposeUIViewController {
    MaterialTheme {
        NetPeekApp(repository = NetPeek.getRepository())
    }
}

/**
 * Opens the inspector list with a close (×) button that invokes [onDismiss].
 * Use this when presenting the inspector modally so the user can dismiss it.
 *
 * Swift usage:
 *   var dismissAction: () -> Void = {}
 *   let vc = NetPeekViewControllerKt.createNetPeekViewController(onDismiss: { dismissAction() })
 *   dismissAction = { vc.dismiss(animated: true, completion: nil) }
 *   vc.modalPresentationStyle = .pageSheet
 *   root.present(vc, animated: true)
 */
fun createNetPeekViewController(onDismiss: () -> Unit): UIViewController = ComposeUIViewController {
    MaterialTheme {
        NetPeekApp(repository = NetPeek.getRepository(), onDismiss = onDismiss)
    }
}

/**
 * Opens the inspector and navigates directly to the given request's detail screen.
 * Use this when presenting the inspector from a notification tap.
 *
 * Swift usage:
 *   let callIdStr = response.notification.request.content.userInfo["netpeek_call_id"] as? String
 *   let callId    = callIdStr.flatMap { Int64($0) } ?? -1
 *   let vc        = NetPeekViewControllerKt.createNetPeekViewController(callId: callId)
 *   present(vc, animated: true)
 */
fun createNetPeekViewController(callId: Long): UIViewController = ComposeUIViewController {
    MaterialTheme {
        NetPeekApp(
            repository    = NetPeek.getRepository(),
            initialCallId = callId
        )
    }
}

/**
 * Opens the inspector at a specific request with a close (×) button.
 * Use this when presenting from a notification tap so the user can dismiss it.
 */
fun createNetPeekViewController(callId: Long, onDismiss: () -> Unit): UIViewController =
    ComposeUIViewController {
        MaterialTheme {
            NetPeekApp(
                repository    = NetPeek.getRepository(),
                initialCallId = callId,
                onDismiss     = onDismiss
            )
        }
    }

