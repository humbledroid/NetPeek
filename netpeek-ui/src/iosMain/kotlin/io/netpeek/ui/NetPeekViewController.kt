package io.netpeek.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.ComposeUIViewController
import io.netpeek.sdk.NetPeek
import platform.Foundation.NSUserActivity
import platform.UIKit.UIApplication
import platform.UIKit.UISceneActivationConditions
import platform.UIKit.UISceneSessionActivationRequest
import platform.UIKit.UIViewController
import platform.UIKit.UIWindowSceneActivationRequestOptions

/**
 * Creates a UIViewController embedding the NetPeek inspector.
 * Use this for modal/push presentation within your existing scene.
 *
 * Usage (Swift):
 *   let vc = NetPeekViewControllerKt.createNetPeekViewController()
 *   present(vc, animated: true)
 */
fun createNetPeekViewController(): UIViewController = ComposeUIViewController {
    MaterialTheme {
        NetPeekApp(repository = NetPeek.getRepository())
    }
}

/**
 * Requests the inspector to open in its own window (separate scene),
 * so it appears alongside your app in the iOS App Switcher.
 *
 * Requirements in your app's Info.plist:
 *   <key>UIApplicationSupportsMultipleScenes</key><true/>
 *
 * Requirements in your SceneDelegate / Info.plist scene config:
 *   - Add a UISceneConfigurations entry with
 *     UISceneConfigurationName = "NetPeekInspector"
 *     UISceneDelegateClassName = your delegate that calls
 *     NetPeekViewControllerKt.createNetPeekViewController()
 *
 * Usage (Swift, iOS 15+):
 *   NetPeekViewControllerKt.requestNetPeekWindow()
 *
 * On iOS 13–14, use the modal approach (createNetPeekViewController) instead.
 */
fun requestNetPeekWindow() {
    if (!UIApplication.sharedApplication.supportsMultipleScenes) {
        // Device/app doesn't support multiple scenes (iPhone pre-iOS 13 or
        // UIApplicationSupportsMultipleScenes not set) — nothing to do here;
        // use createNetPeekViewController() and present modally instead.
        return
    }

    val activity = NSUserActivity("io.netpeek.inspector.open")
    activity.title = "NetPeek Inspector"

    val options = UIWindowSceneActivationRequestOptions()
    // requestPredicate is not set — system will create a new scene or
    // reuse an existing one with the matching userActivity type.

    UIApplication.sharedApplication.requestSceneSessionActivationWithSession(
        session = null,  // null = create new session
        userActivity = activity,
        options = options,
        errorHandler = { error ->
            // Fallback: error opening scene (e.g. on iPhone where multi-window
            // is OS-restricted). Host app should catch this and fall back to
            // presenting createNetPeekViewController() modally.
        }
    )
}
