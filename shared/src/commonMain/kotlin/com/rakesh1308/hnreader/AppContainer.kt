package com.rakesh1308.hnreader

import com.rakesh1308.hnreader.data.local.KeyValueStore
import com.rakesh1308.hnreader.data.remote.HnApi
import com.rakesh1308.hnreader.data.remote.createHnHttpClient
import com.rakesh1308.hnreader.data.repository.BookmarkRepositoryImpl
import com.rakesh1308.hnreader.data.repository.StoryRepositoryImpl
import com.rakesh1308.hnreader.domain.repository.BookmarkRepository
import com.rakesh1308.hnreader.domain.repository.StoryRepository
import com.rakesh1308.hnreader.domain.usecase.GetTopStories
import com.rakesh1308.hnreader.domain.usecase.ObserveBookmarks
import com.rakesh1308.hnreader.domain.usecase.SearchStories
import com.rakesh1308.hnreader.domain.usecase.ToggleBookmark
import com.rakesh1308.hnreader.platform.platformName
import com.rakesh1308.hnreader.presentation.StoryListPresenter
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlin.time.Clock

/**
 * Manual dependency injection: the one place that knows every concrete class.
 * Each app shell creates exactly one of these (through a platform factory) and asks it for presenters.
 */
class AppContainer internal constructor(
    store: KeyValueStore,
    httpClient: HttpClient,
) {
    /** What the shells call: the HTTP client is built internally. */
    constructor(store: KeyValueStore) : this(store, createHnHttpClient())

    private val storyRepository: StoryRepository = StoryRepositoryImpl(HnApi(httpClient))
    private val bookmarkRepository: BookmarkRepository = BookmarkRepositoryImpl(store)

    /** Shown in the UI footer to prove which platform's `actual` is running. */
    val platformLabel: String = platformName()

    fun createStoryListPresenter(): StoryListPresenter = StoryListPresenter(
        getTopStories = GetTopStories(storyRepository),
        searchStories = SearchStories(),
        toggleBookmarkUseCase = ToggleBookmark(bookmarkRepository),
        observeBookmarks = ObserveBookmarks(bookmarkRepository),
        nowEpochSeconds = { Clock.System.now().epochSeconds },
        dispatcher = Dispatchers.Main,
    )
}
