pluginManagement {
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

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "kmp-hn-reader"

include(":shared")     // Kotlin Multiplatform module: domain + data + presentation logic
include(":androidApp") // Android shell: Compose UI, manifest, flavors
// The iOS shell lives in /iosApp and is built by Xcode, which calls Gradle for :shared.
