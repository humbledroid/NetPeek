import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions { jvmTarget = "17" }
        }
    }

    // iOS targets — produces sample_shared.framework which re-exports netpeek-ui + netpeek-sdk
    val iosX64 = iosX64()
    val iosArm64 = iosArm64()
    val iosSimulatorArm64 = iosSimulatorArm64()

    listOf(iosX64, iosArm64, iosSimulatorArm64).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "sample_shared"
            isStatic = true
            // Re-export so Swift app only needs `import sample_shared`
            export(project(":netpeek-ui"))
            export(project(":netpeek-sdk"))
        }
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            // api so both are exported in the iOS framework (transitive export chain doesn't auto-propagate)
            api(project(":netpeek-ui"))
            api(project(":netpeek-sdk"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
        }
        androidMain.dependencies {
            implementation(compose.preview)
            implementation("androidx.activity:activity-compose:1.9.3")
            implementation(libs.ktor.client.android)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.ktor.client.java)
        }
    }
}

// Xcode 16+ rejects underscores in CFBundleIdentifier.
// Kotlin/Native generates "io.netpeek.sample.sample_shared" — patch it to use a hyphen.
tasks.matching { it.name.startsWith("link") && it.name.contains("Framework") && it.name.contains("Ios") }.configureEach {
    doLast {
        fileTree(layout.buildDirectory) {
            include("bin/**/sample_shared.framework/Info.plist")
        }.forEach { plist ->
            val fixed = plist.readText().replace("io.netpeek.sample.sample_shared", "io.netpeek.sample.sample-shared")
            plist.writeText(fixed)
        }
    }
}

android {
    namespace = "io.netpeek.sample.shared"
    compileSdk = 34
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
