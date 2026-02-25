You are building **NetPeek** — a Kotlin Multiplatform (KMP) network inspection SDK, similar to what Chucker does on Android but multiplatform. This is a library + sample app project. Build everything from scratch in the current directory.

---

## Project: netpeek-kmp

### Targets
- Android (minSdk 24)
- iOS (iosArm64 + iosSimulatorArm64 + iosX64)
- JVM (desktop)

### Tech Stack
- Kotlin Multiplatform (KMP) + Compose Multiplatform (CMP) for UI
- Ktor client plugin for network interception (commonMain)
- SQLDelight 2.x for persistence (commonMain)
- Coroutines + Flow for reactive data
- Kotlin Serialization for JSON body pretty-printing
- Build: Gradle (Kotlin DSL), version catalog (libs.versions.toml)

---

## What to Build

### 1. Core SDK Module: `netpeek-sdk`

**Ktor Plugin (`commonMain`):**
- A Ktor `HttpClientPlugin` called `NetPeekPlugin`
- Intercepts every request + response pair
- Captures:
  - url, method, request headers, request body
  - status code, response headers, response body
  - duration (ms), timestamp, isError flag
- Stores captured calls via `NetworkCallRepository`
- Config options: `enabled`, `maxStoredCalls`, `redactHeaders` (list of header names to blank out, e.g. Authorization)

**Data Model (`commonMain`):**
```kotlin
data class NetworkCall(
  val id: Long,
  val url: String,
  val method: String,
  val requestHeaders: String,  // JSON map
  val requestBody: String?,
  val responseCode: Int?,
  val responseHeaders: String?,
  val responseBody: String?,
  val durationMs: Long?,
  val timestamp: Long,          // epoch millis
  val isError: Boolean
)
```

**SQLDelight (`commonMain`):**
- Database: `NetPeekDatabase`
- Table: `network_calls` matching NetworkCall fields
- Queries: insertCall, selectAll (ordered by timestamp desc), selectById, deleteAll, deleteOlderThan, countAll

**Repository (`commonMain`):**
- `NetworkCallRepository` interface + `NetworkCallRepositoryImpl`
- `getAllCalls(): Flow<List<NetworkCall>>`
- `getCallById(id: Long): NetworkCall?`
- `insert(call: NetworkCall)`
- `clearAll()`

**Platform DB drivers (expect/actual):**
- Android: `AndroidSqliteDriver`
- JVM: `JdbcSqliteDriver`
- iOS: `NativeSqliteDriver`

---

### 2. UI Module: `netpeek-ui` (Compose Multiplatform)

**Screens:**
1. **NetPeekListScreen** — scrollable list of captured calls
   - Each row: method badge (color-coded GET/POST/etc), URL (truncated), status code, duration, timestamp
   - Status color: green 2xx, yellow 3xx, red 4xx/5xx, grey for in-flight/error
   - Top bar: title "NetPeek", clear-all button, live call count badge
   - Search bar to filter by URL

2. **NetPeekDetailScreen** — tap a row to see full detail
   - Tabs: Overview | Request | Response
   - Overview: url, method, status, duration, timestamp
   - Request: headers list, body (pretty-printed if JSON, else raw)
   - Response: headers list, body (pretty-printed if JSON, else raw)
   - Back button

**Entry Points:**
- Android: `NetPeekActivity` — hosts CMP content
- JVM: `launchNetPeekWindow()` — opens a `singleWindowApplication`
- iOS: `NetPeekViewController` — UIViewController hosting Compose view

**Public API:**
```kotlin
object NetPeek {
  fun install(client: HttpClientConfig<*>, config: NetPeekConfig = NetPeekConfig())
  fun getRepository(): NetworkCallRepository
  // Android
  fun getLaunchIntent(context: Context): Intent
  // JVM
  fun launchWindow()
}
```

---

### 3. Sample App: `sample-android`
- Minimal Android app (single Activity)
- Ktor client with NetPeekPlugin installed
- 4 buttons: GET, POST, 404, Timeout — each fires a sample request
- FAB that launches NetPeekActivity

---

## File Structure

```
netpeek-kmp/
  gradle/
    libs.versions.toml
    wrapper/gradle-wrapper.properties
  netpeek-sdk/
    src/
      commonMain/kotlin/io/netpeek/sdk/
      androidMain/kotlin/io/netpeek/sdk/
      iosMain/kotlin/io/netpeek/sdk/
      jvmMain/kotlin/io/netpeek/sdk/
      commonTest/kotlin/
    build.gradle.kts
  netpeek-ui/
    src/
      commonMain/kotlin/io/netpeek/ui/
      androidMain/kotlin/io/netpeek/ui/
      iosMain/kotlin/io/netpeek/ui/
      jvmMain/kotlin/io/netpeek/ui/
    build.gradle.kts
  sample-android/
    src/main/
    build.gradle.kts
  build.gradle.kts
  settings.gradle.kts
  README.md
```

---

## Version Catalog (gradle/libs.versions.toml)

```toml
[versions]
kotlin = "2.0.21"
agp = "8.5.2"
compose-multiplatform = "1.7.0"
ktor = "3.0.3"
sqldelight = "2.0.2"
coroutines = "1.9.0"
serialization = "1.7.3"

[libraries]
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-android = { module = "io.ktor:ktor-client-android", version.ref = "ktor" }
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
ktor-client-java = { module = "io.ktor:ktor-client-java", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
sqldelight-runtime = { module = "app.cash.sqldelight:runtime", version.ref = "sqldelight" }
sqldelight-coroutines = { module = "app.cash.sqldelight:coroutines-extensions", version.ref = "sqldelight" }
sqldelight-android = { module = "app.cash.sqldelight:android-driver", version.ref = "sqldelight" }
sqldelight-jvm = { module = "app.cash.sqldelight:sqlite-driver", version.ref = "sqldelight" }
sqldelight-native = { module = "app.cash.sqldelight:native-driver", version.ref = "sqldelight" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
android-library = { id = "com.android.library", version.ref = "agp" }
android-application = { id = "com.android.application", version.ref = "agp" }
compose-multiplatform = { id = "org.jetbrains.compose", version.ref = "compose-multiplatform" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
sqldelight = { id = "app.cash.sqldelight", version.ref = "sqldelight" }
```

---

## Quality Bar
- All code must compile (no placeholder TODO stubs that break compilation)
- Use expect/actual correctly for platform drivers
- Proper coroutine scoping (no GlobalScope)
- SQLDelight .sq file must be valid syntax
- Compose screens must use Material3
- README.md at root: what it is, how to install, how to use in 3 steps

---

When completely finished, run this exact shell command to notify the orchestrator:
openclaw system event --text "Done: NetPeek KMP SDK built"
