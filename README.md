# kmp-hn-reader

A small **Kotlin Multiplatform** app that reads the Hacker News front page, searches it, and lets you
save stories for offline reading. Built to learn and show **clean architecture on KMP**:
one shared Kotlin module, a Jetpack Compose screen on Android, a SwiftUI screen on iOS, and **one small
Compose Multiplatform widget (`SummaryCard`) written once and drawn on both**.

```text
 androidApp (Compose)      iosApp (SwiftUI)
        └────────┬────────────────┘
                 ▼   both draw the same StoryListState
        shared (Kotlin Multiplatform)
          presentation  →  domain  ←  data
```

Read [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the layer map and a walkthrough of one tap through every layer.

## What it does

| Use case | Layer | Talks to |
|---|---|---|
| `GetTopStories` | domain → data | Hacker News API through Ktor (server interaction) |
| `SearchStories` | domain | nothing (pure function) |
| `ToggleBookmark` / `ObserveBookmarks` | domain → data | SharedPreferences (Android) / NSUserDefaults (iOS) |

## Project layout

```text
shared/                     the KMP module
  src/commonMain/           domain, data, presentation, and ui/SummaryCard.kt (the shared Compose widget)
  src/androidMain/          Android actuals (SharedPreferences, platform name)
  src/iosMain/              iOS actuals (NSUserDefaults, platform name) + SummaryCardController (hosts the shared UI)
  src/jvmMain/              lets commonTest run as fast plain-JVM tests
  src/commonTest/           31 tests that run on every target
androidApp/                 Android shell: manifest, Gradle, Compose screen
iosApp/                     iOS shell: SwiftUI + project.yml (XcodeGen spec)
```

## Run it

**Android** (Windows, macOS or Linux): open the folder in Android Studio and run `androidApp`, or:

```sh
gradle wrapper --gradle-version 8.14.3   # once: creates ./gradlew (the wrapper jar is not committed)
./gradlew :androidApp:installDebug
./gradlew :shared:jvmTest :shared:testDebugUnitTest
```

**iOS** (needs a Mac with Xcode):

```sh
gradle wrapper --gradle-version 8.14.3   # Xcode's Run Script phase calls ./gradlew
brew install xcodegen
cd iosApp && xcodegen generate && open iosApp.xcodeproj
```

Press Run. A script phase asks Gradle to build the Kotlin framework (`Shared`), then Xcode compiles the Swift.

## What has and has not been verified

Checked on Linux with JDK 21, Gradle 8.14.3, Kotlin 2.4.20, AGP 8.13.2:

- `:shared:jvmTest`: 31 tests pass
- `:shared:testDebugUnitTest`: the same 31 tests pass on the Android unit-test runtime
- `:androidApp:assembleDebug` and `:androidApp:assembleRelease`: both build (the release APK is unsigned)

**Not verified:**

- The app has **not been launched** on an emulator or a device, so no screen, including the shared `SummaryCard`, has been seen running. (The Android APK was inspected and does contain it.)
- The server layer is tested against a fake server (Ktor `MockEngine`), not the live Hacker News API from inside the app.
- **Everything iOS is unverified**: `shared/src/iosMain`, the Swift files and `iosApp/project.yml` need macOS and Xcode.
  The `ios` job in `.github/workflows/ci.yml` builds them and runs the shared tests on the simulator;
  treat the iOS side as "written, not yet proven" until that job is green. That includes the shared Compose card on iOS
  (`SummaryCardController`, the `UIViewControllerRepresentable` bridge, and the Info.plist key Compose needs).

**Known noise:** during `assembleRelease`, Android Lint (bundled with AGP 8.13) prints `Module was compiled with an
incompatible version of Kotlin` because it embeds an older Kotlin than 2.4. The build still succeeds.
Moving to AGP 9 is the fix; it was left out to keep this project simple.

## Deliberately left out

Free/paid flavors, a DI framework (Koin), a real database (SQLDelight), navigation, a fully shared Compose UI
(only one widget is shared), release signing. Each is a good next step once this is understood.

## Why Compose Multiplatform 1.11.1 and not the newest

CMP 1.12.x pulls androidx Compose 1.12, which requires AGP 9.1+ and compileSdk 37. This project is on AGP 8.13.2
and compileSdk 36, so it pins CMP 1.11.1. `material3` has its own version line (1.9.0 here). Upgrading AGP is the
way to move to newer Compose.
