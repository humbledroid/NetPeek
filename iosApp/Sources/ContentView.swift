import SwiftUI
import UIKit
import sample_shared

/// SwiftUI bridge for the Kotlin sample screen.
///
/// On iPad (supportsMultipleScenes = true):  tapping the inspector button opens it
/// as a separate scene in the app switcher via SwiftUI's openWindow(id:).
///
/// On iPhone (supportsMultipleScenes = false): openWindow is unavailable, so nil is
/// passed and Kotlin falls back to showing the inspector inline within the same VC.
struct ContentView: View {
    @Environment(\.openWindow) private var openWindow

    var body: some View {
        SampleRepresentable(
            onOpenInspector: UIApplication.shared.supportsMultipleScenes
                ? { openWindow(id: "netpeek-inspector") }
                : nil
        )
        .ignoresSafeArea()
    }
}

private struct SampleRepresentable: UIViewControllerRepresentable {
    let onOpenInspector: (() -> Void)?

    func makeUIViewController(context: Context) -> UIViewController {
        if let onOpenInspector {
            SampleViewControllerKt.createSampleViewController(onOpenInspector: onOpenInspector)
        } else {
            // iPhone / no multi-scene: Kotlin handles inline fallback
            SampleViewControllerKt.createSampleViewController()
        }
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
