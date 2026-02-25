# NetPeek 🔍

A Kotlin Multiplatform (KMP) network inspection SDK — like Chucker, but for all platforms.

## Platforms
- ✅ Android
- ✅ iOS (iosArm64, iosSimulatorArm64, iosX64)
- ✅ JVM Desktop

## Setup

Add to your `build.gradle.kts`:
```kotlin
dependencies {
    implementation("io.netpeek:netpeek-sdk:1.0.0")
    implementation("io.netpeek:netpeek-ui:1.0.0")
}
```

## Usage

### 1. Initialize (once, at app start)
```kotlin
// Android
NetPeek.init(DatabaseDriverFactory(context))

// JVM
NetPeek.init(DatabaseDriverFactory())

// iOS
NetPeek.init(DatabaseDriverFactory())
```

### 2. Install the Ktor plugin
```kotlin
val client = HttpClient {
    NetPeek.install(this)
}
```

### 3. Launch the inspector UI
```kotlin
// Android
startActivity(NetPeekActivity.newIntent(context))

// JVM
launchNetPeekWindow()

// iOS (SwiftUI)
let vc = NetPeekViewControllerKt.createNetPeekViewController()
present(vc, animated: true)
```

## Features
- 🔌 Ktor client plugin — zero-config interception
- 💾 SQLDelight persistence — survives app restarts
- 🎨 Compose Multiplatform UI — same UI on all platforms
- 🔍 Search + filter requests
- 🎨 Color-coded status codes and HTTP methods
- 🔒 Automatic header redaction (Authorization, Cookie, etc.)
