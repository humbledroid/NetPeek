import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvm()

    sourceSets {
        jvmMain.dependencies {
            implementation(project(":netpeek-sdk"))
            implementation(project(":netpeek-ui"))
            implementation(project(":sample-shared"))
            implementation(compose.desktop.currentOs)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.foundation)
            implementation(libs.ktor.client.java)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

compose.desktop {
    application {
        mainClass = "io.netpeek.sample.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "NetPeek Sample"
            packageVersion = "1.0.0"
        }
    }
}
