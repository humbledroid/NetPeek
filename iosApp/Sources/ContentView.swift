import SwiftUI
import UIKit
import netpeek_sdk
import netpeek_ui

struct ContentView: View {

    @State private var log = "Tap a button to fire a request"
    @State private var loading = false
    @State private var showInspector = false

    // Ktor HttpClient created once — NetPeek is already installed via SampleApp.init()
    private let client = KtorClientFactory.shared.makeClient()

    var body: some View {
        NavigationStack {
            VStack(spacing: 20) {

                // App header
                VStack(alignment: .leading, spacing: 4) {
                    Text("My iOS App")
                        .font(.largeTitle.bold())
                    Text("Sample app using the NetPeek SDK")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal)

                Divider()

                // API buttons
                VStack(spacing: 12) {
                    Text("API Endpoints")
                        .font(.headline)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal)

                    HStack(spacing: 10) {
                        RequestButton(label: "GET /get", color: .green, enabled: !loading) {
                            fire("https://httpbin.org/get", method: "GET")
                        }
                        RequestButton(label: "POST /post", color: .blue, enabled: !loading) {
                            fire("https://httpbin.org/post", method: "POST",
                                 body: #"{"platform":"iOS"}"#)
                        }
                    }
                    .padding(.horizontal)

                    HStack(spacing: 10) {
                        RequestButton(label: "404 Error", color: .red, enabled: !loading) {
                            fire("https://httpbin.org/status/404", method: "GET")
                        }
                        RequestButton(label: "⏱ Timeout", color: .orange, enabled: !loading) {
                            fire("https://httpbin.org/delay/10", method: "GET", timeoutMs: 2000)
                        }
                    }
                    .padding(.horizontal)
                }

                // Response log
                HStack {
                    if loading {
                        ProgressView().scaleEffect(0.8)
                    }
                    Text(log)
                        .font(.system(.caption, design: .monospaced))
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.leading)
                    Spacer()
                }
                .padding(12)
                .background(.quaternary, in: RoundedRectangle(cornerRadius: 10))
                .padding(.horizontal)

                Spacer()

                // Hint
                Text("💡 Tap \"Network Inspector\" to see all captured traffic")
                    .font(.caption)
                    .foregroundStyle(.tertiary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)
            }
            .padding(.top)
            .navigationTitle("")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        showInspector = true
                    } label: {
                        Label("Network Inspector", systemImage: "network")
                    }
                }
            }
            // Inspector opens as a full-screen sheet — separate from the app UI
            .sheet(isPresented: $showInspector) {
                NetPeekSheetView()
            }
        }
    }

    // Fire a request through the KMP Ktor client (NetPeek intercepts automatically)
    private func fire(_ urlString: String, method: String, body: String? = nil, timeoutMs: Int64 = 10000) {
        loading = true
        log = "→ \(method) \(urlString)"
        Task {
            do {
                let result = try await KtorClientFactory.shared.request(
                    url: urlString,
                    method: method,
                    body: body,
                    timeoutMs: timeoutMs
                )
                log = "✓ \(method) \(urlString) → \(result)"
            } catch {
                log = "✗ \(error.localizedDescription)"
            }
            loading = false
        }
    }
}

// MARK: - NetPeek Inspector Sheet

struct NetPeekSheetView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // This calls the KMP Compose function that creates the inspector UI
        NetPeekViewControllerKt.createNetPeekViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

// MARK: - Subviews

struct RequestButton: View {
    let label: String
    let color: Color
    let enabled: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.system(.caption, design: .monospaced).weight(.semibold))
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
        }
        .buttonStyle(.borderedProminent)
        .tint(color)
        .disabled(!enabled)
    }
}

#Preview {
    ContentView()
}
