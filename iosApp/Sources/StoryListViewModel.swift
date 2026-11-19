import SwiftUI
import Shared

/// Swift wrapper around the shared Kotlin presenter. It does two jobs only:
///   1. republish Kotlin's state so SwiftUI redraws (@Published)
///   2. tie the Kotlin object's lifetime to this Swift object (nobody else will call close())
@MainActor
final class StoryListViewModel: ObservableObject {
    @Published private(set) var state: StoryListState

    private let presenter: StoryListPresenter
    private var subscription: Subscription?

    init(container: AppContainer) {
        presenter = container.createStoryListPresenter()
        state = StoryListState(items: [], isLoading: true, errorMessage: nil, query: "", savedOnly: false)
        // The presenter runs on Kotlin's Dispatchers.Main, so this callback arrives on the main thread.
        subscription = presenter.watch { [weak self] newState in
            self?.state = newState   // [weak self]: avoid a Swift<->Kotlin retain cycle
        }
    }

    deinit {
        subscription?.cancel()   // stop receiving
        presenter.close()        // cancel Kotlin coroutines (Android does this in onCleared())
    }

    func refresh() { presenter.refresh() }
    func setQuery(_ query: String) { presenter.onQueryChange(query: query) }
    func setSavedOnly(_ savedOnly: Bool) { presenter.onSavedOnlyChange(savedOnly: savedOnly) }
    func toggleBookmark(_ id: Int64) { presenter.toggleBookmark(id: id) }
}
