# Architecture

One rule drives everything: **source-code dependencies point inward.**

```text
┌────────────────────────────────────────────────────────────────────────┐
│  UI SHELLS (one per platform, thin)                                    │
│    androidApp/  Jetpack Compose          iosApp/  SwiftUI              │
│        │                                      │                        │
│        ▼  both draw the same StoryListState   ▼                        │
├────────────────────────────────────────────────────────────────────────┤
│  shared/commonMain  (Kotlin, runs on Android AND iOS)                  │
│                                                                        │
│   PRESENTATION   StoryListPresenter → StoryListState                   │
│        │ uses                                                          │
│        ▼                                                               │
│   DOMAIN         use cases  +  models  +  repository INTERFACES        │
│        ▲ implemented by                                                │
│        │                                                               │
│   DATA           Ktor API  +  DTOs  +  repository IMPLEMENTATIONS      │
└────────────────────────────────────────────────────────────────────────┘
        ▲ platform pieces plugged in through expect/actual + interfaces
   shared/androidMain (SharedPreferences)   shared/iosMain (NSUserDefaults)
```

Imports allowed: `presentation → domain ← data`. The **domain imports nothing** from the other layers
and nothing platform-specific. That is why it can live in `commonMain`.

## Where things live

| Layer | Folder (under `shared/src/commonMain/kotlin/.../`) | Contents |
|---|---|---|
| Domain | `domain/model` | `Story`, `Outcome`, `AppError` |
| Domain | `domain/repository` | `StoryRepository`, `BookmarkRepository` (interfaces only) |
| Domain | `domain/usecase` | `GetTopStories`, `SearchStories`, `ToggleBookmark`, `ObserveBookmarks` |
| Data | `data/remote` | `HnApi` (Ktor), `ItemDto`, JSON + HTTP client setup |
| Data | `data/repository` | `StoryRepositoryImpl`, `BookmarkRepositoryImpl` |
| Data | `data/local` | `KeyValueStore` interface + in-memory fake |
| Presentation | `presentation` | `StoryListPresenter`, `StoryListState`, `StoryItem` |
| Wiring | `AppContainer.kt` | manual dependency injection |
| Platform seam | `platform/Platform.kt` | `expect fun platformName()` |

Platform source sets:

| Source set | What it adds |
|---|---|
| `commonMain` | everything above |
| `androidMain` | `actual platformName()`, `SharedPrefsKeyValueStore`, `AndroidAppContainerFactory`, OkHttp engine |
| `iosMain` | `actual platformName()`, `UserDefaultsKeyValueStore`, `IosAppContainerFactory`, Darwin engine |
| `jvmMain` | `actual platformName()`, OkHttp engine (lets tests run as plain JVM tests) |
| `commonTest` | all tests: they run on every target |

## One tap, through every layer

User taps the star on a story:

```text
Compose / SwiftUI button
   └─► presenter.toggleBookmark(id)                      presentation
        └─► ToggleBookmark(story)                        domain (use case)
             └─► BookmarkRepository.add / remove         domain (interface)
                  └─► BookmarkRepositoryImpl             data (implements the interface)
                       └─► KeyValueStore.putString       data/local (interface)
                            └─► SharedPreferences | NSUserDefaults   platform actual

...and back up, as data:
   bookmarks Flow changes ─► presenter recombines ─► new StoryListState ─► UI redraws
```

Calls go outward at runtime, but every `import` points inward. The domain *declares* what it needs
(`BookmarkRepository`) and the data layer *provides* it. That reversal is called dependency inversion.

## iOS side: the same layers

```text
iosApp/Sources/
  HnReaderApp.swift          @main, builds the AppContainer         (≈ Application class)
  StoryListViewModel.swift   ObservableObject around the presenter  (≈ androidx ViewModel)
  ContentView.swift          SwiftUI screen, draws StoryListState   (≈ Compose screen)
```

The Swift side contains no business logic. If a rule changes (how search matches, how errors read),
you change Kotlin once and both apps change.

## Design decisions worth knowing

- **Errors are values.** Use cases and repositories return `Outcome`, not exceptions, because an uncaught
  Kotlin exception crashes an iOS app unless every function is annotated `@Throws`.
- **DTOs are separate from domain models.** `ItemDto` and `StoredStory` carry serialization annotations;
  `Story` stays plain Kotlin.
- **Manual DI.** One `AppContainer`, no framework. Koin is the usual next step once it grows.
- **Lifetime is explicit.** The presenter owns coroutines. Android closes it in `onCleared()`, iOS in `deinit`.
- **`KeyValueStore` is deliberately basic.** SQLDelight is the production answer for real data.
