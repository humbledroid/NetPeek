import SwiftUI
import UserNotifications
import netpeek_sdk
import netpeek_ui

@main
struct SampleApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    init() {
        // 1. Init the NetPeek SDK
        NetPeek.shared.doInit(driverFactory: DatabaseDriverFactory(), config: NetPeekConfig())

        // 2. Request notification permission + start listening
        NetPeekNotifier.shared.requestPermission(provisional: false)
        NetPeekNotifier.shared.start()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

// MARK: - AppDelegate for notification handling

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        return true
    }

    func applicationDidBecomeActive(_ application: UIApplication) {
        // Reset badge when user opens the app
        NetPeekNotifier.shared.resetBadge()
    }

    func applicationWillTerminate(_ application: UIApplication) {
        NetPeekNotifier.shared.stop()
    }

    // Show notifications even when the app is in the foreground
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .badge, .sound])
    }

    // Handle tapping a notification or its action buttons
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        // Let NetPeek handle its own notification actions
        if NetPeekViewControllerKt.handleNotificationResponse(response: response) {
            // "Open Inspector" action — present the inspector
            if response.actionIdentifier == "NETPEEK_OPEN" {
                DispatchQueue.main.async {
                    guard let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                          let root  = scene.windows.first?.rootViewController else { return }
                    let inspector = NetPeekViewControllerKt.createNetPeekViewController()
                    inspector.modalPresentationStyle = .formSheet
                    root.present(inspector, animated: true)
                }
            }
            completionHandler()
            return
        }
        completionHandler()
    }
}
