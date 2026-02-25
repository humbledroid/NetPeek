import SwiftUI
import netpeek_sdk
import netpeek_ui

@main
struct SampleApp: App {

    init() {
        // 1. Init NetPeek (identical to Android/Desktop — just 1 line)
        NetPeek.shared.doInit(driverFactory: DatabaseDriverFactory(), config: NetPeekConfig())
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
