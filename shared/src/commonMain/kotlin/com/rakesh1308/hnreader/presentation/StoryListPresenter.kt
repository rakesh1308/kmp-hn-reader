package com.rakesh1308.hnreader.presentation

import com.rakesh1308.hnreader.domain.model.AppError
import com.rakesh1308.hnreader.domain.model.Outcome
import com.rakesh1308.hnreader.domain.model.Story
import com.rakesh1308.hnreader.domain.usecase.GetTopStories
import com.rakesh1308.hnreader.domain.usecase.ObserveBookmarks
import com.rakesh1308.hnreader.domain.usecase.SearchStories
import com.rakesh1308.hnreader.domain.usecase.ToggleBookmark
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One row, already formatted for display. UIs do no logic: they just draw this. */
data class StoryItem(
    val id: Long,
    val title: String,
    val host: String,
    val meta: String,
    val url: String,
    val isBookmarked: Boolean,
)

/** Everything a screen needs, in one immutable value. */
data class StoryListState(
    val items: List<StoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val query: String = "",
    val savedOnly: Boolean = false,
)

/** Handle returned by [StoryListPresenter.watch]; call [cancel] to stop receiving updates. */
interface Subscription {
    fun cancel()
}

/**
 * Platform-neutral "ViewModel". It owns the screen logic and exposes one [StateFlow].
 *  - Android wraps it in an androidx ViewModel and calls [close] from onCleared().
 *  - iOS wraps it in an ObservableObject, subscribes with [watch], and calls [close] in deinit.
 *
 * Nothing calls close() for us on iOS, so the lifetime is wired explicitly in each shell.
 */
class StoryListPresenter internal constructor(
    private val getTopStories: GetTopStories,
    private val searchStories: SearchStories,
    private val toggleBookmarkUseCase: ToggleBookmark,
    observeBookmarks: ObserveBookmarks,
    private val nowEpochSeconds: () -> Long,
    dispatcher: CoroutineDispatcher,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private data class UiFlags(
        val isLoading: Boolean = false,
        val error: String? = null,
        val query: String = "",
        val savedOnly: Boolean = false,
    )

    private val loaded = MutableStateFlow<List<Story>>(emptyList())
    private val ui = MutableStateFlow(UiFlags())
    private val bookmarks: StateFlow<List<Story>> =
        observeBookmarks().stateIn(scope, SharingStarted.Eagerly, emptyList())

    val state: StateFlow<StoryListState> =
        combine(loaded, bookmarks, ui) { stories, saved, flags ->
            val savedIds = saved.mapTo(HashSet()) { it.id }
            val source = if (flags.savedOnly) saved else stories
            val now = nowEpochSeconds()
            StoryListState(
                items = searchStories(source, flags.query).map { it.toItem(savedIds, now) },
                isLoading = flags.isLoading,
                errorMessage = flags.error,
                query = flags.query,
                savedOnly = flags.savedOnly,
            )
        }.stateIn(scope, SharingStarted.Eagerly, StoryListState())

    init {
        refresh()
    }

    fun refresh() {
        if (ui.value.isLoading) return
        ui.update { it.copy(isLoading = true, error = null) }
        scope.launch {
            when (val result = getTopStories()) {
                is Outcome.Success -> {
                    loaded.value = result.value
                    ui.update { it.copy(isLoading = false) }
                }
                is Outcome.Failure ->
                    ui.update { it.copy(isLoading = false, error = result.error.userMessage()) }
            }
        }
    }

    fun onQueryChange(query: String) = ui.update { it.copy(query = query) }

    fun onSavedOnlyChange(savedOnly: Boolean) = ui.update { it.copy(savedOnly = savedOnly) }

    fun toggleBookmark(id: Long) {
        val story = (loaded.value + bookmarks.value).firstOrNull { it.id == id } ?: return
        scope.launch { toggleBookmarkUseCase(story) }
    }

    /** Callback-style observation, friendly to Swift (no generics or Flow needed on the other side). */
    fun watch(onState: (StoryListState) -> Unit): Subscription {
        val job = scope.launch { state.collect { onState(it) } }
        return object : Subscription {
            override fun cancel() = job.cancel()
        }
    }

    /** Stops all work started by this presenter. Call when the screen is gone. */
    fun close() = scope.cancel()
}

private fun Story.toItem(savedIds: Set<Long>, nowSeconds: Long) = StoryItem(
    id = id,
    title = title,
    host = host ?: "news.ycombinator.com",
    meta = "$score points · $commentCount comments · ${formatAge(nowSeconds, postedAtEpochSeconds)}",
    // Text posts (Ask HN...) have no external URL: link to the discussion page instead.
    url = url ?: "https://news.ycombinator.com/item?id=$id",
    isBookmarked = id in savedIds,
)

internal fun formatAge(nowSeconds: Long, thenSeconds: Long): String {
    val seconds = (nowSeconds - thenSeconds).coerceAtLeast(0)
    return when {
        seconds < 60 -> "just now"
        seconds < 3_600 -> "${seconds / 60}m ago"
        seconds < 86_400 -> "${seconds / 3_600}h ago"
        else -> "${seconds / 86_400}d ago"
    }
}

internal fun AppError.userMessage(): String = when (this) {
    AppError.Network -> "Can't reach Hacker News. Check your connection and try again."
    is AppError.Server -> "Hacker News returned an error (HTTP $statusCode)."
    AppError.InvalidResponse -> "Got an unexpected response from the server."
    is AppError.Unknown -> "Something went wrong."
}
