import SwiftUI
import UserNotifications
import sample_shared  // re-exports netpeek-ui (which re-exports netpeek-sdk)

@main
struct SampleApp: App {

    @Environment(\.scenePhase) private var scenePhase

    // Retained for the app's lifetime as UNUserNotificationCenter.delegate.
    // Stored as a let on the App struct — SwiftUI retains the App for the process lifetime.
    private let notifDelegate = NetPeekNotificationDelegate()

    init() {
        // 1. Init the NetPeek SDK (must happen before the Compose ViewController is created)
        NetPeek.shared.doInit(driverFactory: DatabaseDriverFactory())

        // 2. Set notification delegate, request permission, start listening for new calls
        UNUserNotificationCenter.current().delegate = notifDelegate
        NetPeekNotifier.shared.requestPermission(provisional: false)
        NetPeekNotifier.shared.start()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onChange(of: scenePhase) { phase in
                    if phase == .active { NetPeekNotifier.shared.resetBadge() }
                }
        }

        // Inspector opens as a separate scene visible in the iOS app switcher.
        // Triggered by SwiftUI's openWindow(id: "netpeek-inspector") from ContentView.
        WindowGroup(id: "netpeek-inspector") {
            NetPeekInspectorView()
        }
    }
}

// MARK: - Inspector window views

/// SwiftUI host for the NetPeek inspector window scene.
/// `@Environment(\.dismiss)` closes the scene when called from within a WindowGroup.
private struct NetPeekInspectorView: View {
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NetPeekInspectorRepresentable(onDismiss: { dismiss() })
            .ignoresSafeArea()
    }
}

private struct NetPeekInspectorRepresentable: UIViewControllerRepresentable {
    let onDismiss: () -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        NetPeekViewControllerKt.createNetPeekViewController(onDismiss: onDismiss)
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

// MARK: - UNUserNotificationCenter delegate

/// Handles notification presentation and tap actions for NetPeek notifications.
/// Lives outside the AppDelegate so SwiftUI retains full control of scene lifecycle.
private class NetPeekNotificationDelegate: NSObject, UNUserNotificationCenterDelegate {

    // Show notifications even when the app is in the foreground
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .badge, .sound])
    }

    // Handle tapping a notification — present the inspector at the relevant request
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        guard NetPeekNotifierKt.handleNotificationResponse(response: response) else {
            completionHandler()
            return
        }

        let isOpenAction = response.actionIdentifier == NetPeekNotifier.shared.ACTION_OPEN
                        || response.actionIdentifier == UNNotificationDefaultActionIdentifier

        if isOpenAction {
            let userInfo  = response.notification.request.content.userInfo
            let callIdStr = userInfo["netpeek_call_id"] as? String
            let callId    = callIdStr.flatMap { Int64($0) } ?? -1

            DispatchQueue.main.async {
                guard let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                      let root  = scene.windows.first?.rootViewController else { return }

                // Use a deferred var so the dismiss closure can reference the VC after creation.
                var dismissAction: () -> Void = {}
                let inspector: UIViewController = callId > 0
                    ? NetPeekViewControllerKt.createNetPeekViewController(callId: callId, onDismiss: { dismissAction() })
                    : NetPeekViewControllerKt.createNetPeekViewController(onDismiss: { dismissAction() })
                dismissAction = { inspector.dismiss(animated: true, completion: nil) }

                // pageSheet supports both the close button (×) and swipe-to-dismiss.
                inspector.modalPresentationStyle = .pageSheet
                root.present(inspector, animated: true)
            }
        }
        completionHandler()
    }
}
