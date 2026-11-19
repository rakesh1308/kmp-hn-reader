import SwiftUI
import Shared   // the Kotlin framework built by Gradle (shared/build.gradle.kts: baseName = "Shared")

/// iOS's equivalent of Android's `Application` + launcher activity: `@main` is the entry point.
@main
struct HnReaderApp: App {
    // Kotlin `object IosAppContainerFactory` is reached through `.shared` from Swift.
    private let container = IosAppContainerFactory.shared.create()

    var body: some Scene {
        WindowGroup {
            ContentView(container: container)
        }
    }
}
