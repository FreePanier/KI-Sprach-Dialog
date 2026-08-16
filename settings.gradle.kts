pluginManagement {
    // Force SDK location for AGP
    val sdkPath = "C:/Users/Panier/AppData/Local/Android/Sdk"
    System.setProperty("android.home", sdkPath)
    System.setProperty("android.sdk.path", sdkPath)

    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SprachBruecke"
include(":app")
