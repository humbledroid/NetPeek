rootProject.name = "netpeek-kmp"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(":netpeek-sdk")
include(":netpeek-ui")
include(":sample-shared")
include(":sample-android")
include(":sample-desktop")
