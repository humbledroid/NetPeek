import Foundation
import netpeek_sdk
import Ktor

/// Thin Swift wrapper around the KMP Ktor HttpClient.
/// NetPeek is already installed because NetPeek.shared.doInit() was called in SampleApp.init().
@MainActor
final class KtorClientFactory {

    static let shared = KtorClientFactory()

    private let client: Ktor_client_coreHttpClient

    private init() {
        client = Ktor_client_coreHttpClient(engine: IosHttpEngineFactory()) { config in
            // NetPeek plugin is installed here — same API as Android/Desktop
            NetPeek.shared.install(clientConfig: config, config: NetPeekConfig())
        }
    }

    func makeClient() -> Ktor_client_coreHttpClient { client }

    /// Fire an HTTP request and return the status code as a string.
    func request(
        url urlString: String,
        method: String = "GET",
        body: String? = nil,
        timeoutMs: Int64 = 10_000
    ) async throws -> String {
        let url = URL(string: urlString)!
        var request = URLRequest(url: url, timeoutInterval: Double(timeoutMs) / 1000)
        request.httpMethod = method
        if let body = body {
            request.httpBody = body.data(using: .utf8)
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }

        // Use URLSession directly — NetPeek on iOS captures via Ktor, not URLSession.
        // For a real integration, use the KMP Ktor client directly.
        let (_, response) = try await URLSession.shared.data(for: request)
        let status = (response as? HTTPURLResponse)?.statusCode ?? 0
        return "\(status) \(HTTPURLResponse.localizedString(forStatusCode: status))"
    }
}
