plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions { jvmTarget = "17" }
        }
    }
    
    // iOS targets with framework configuration for Xcode integration
    val iosX64 = iosX64()
    val iosArm64 = iosArm64()
    val iosSimulatorArm64 = iosSimulatorArm64()
    
    // Configure frameworks for iOS
    listOf(iosX64, iosArm64, iosSimulatorArm64).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "netpeek_sdk"
            isStatic = true
        }
    }
    
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.android)
            implementation(libs.sqldelight.android)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.java)
            implementation(libs.sqldelight.jvm)
        }
    }
}

android {
    namespace = "io.netpeek.sdk"
    compileSdk = 34
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

sqldelight {
    databases {
        create("NetPeekDatabase") {
            packageName.set("io.netpeek.db")
        }
    }
}

// Xcode 16+ rejects underscores in CFBundleIdentifier.
// Kotlin/Native generates "io.netpeek.netpeek_sdk" — patch it to use a hyphen after every link.
tasks.matching { it.name.startsWith("link") && it.name.contains("Framework") && it.name.contains("Ios") }.configureEach {
    doLast {
        fileTree(layout.buildDirectory) {
            include("bin/**/netpeek_sdk.framework/Info.plist")
        }.forEach { plist ->
            val fixed = plist.readText().replace("io.netpeek.netpeek_sdk", "io.netpeek.netpeek-sdk")
            plist.writeText(fixed)
        }
    }
}
