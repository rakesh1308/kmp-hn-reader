package com.rakesh1308.hnreader.presentation

import com.rakesh1308.hnreader.data.local.InMemoryKeyValueStore
import com.rakesh1308.hnreader.data.repository.BookmarkRepositoryImpl
import com.rakesh1308.hnreader.domain.model.AppError
import com.rakesh1308.hnreader.domain.model.Outcome
import com.rakesh1308.hnreader.domain.model.Story
import com.rakesh1308.hnreader.domain.repository.StoryRepository
import com.rakesh1308.hnreader.domain.usecase.GetTopStories
import com.rakesh1308.hnreader.domain.usecase.ObserveBookmarks
import com.rakesh1308.hnreader.domain.usecase.SearchStories
import com.rakesh1308.hnreader.domain.usecase.ToggleBookmark
import com.rakesh1308.hnreader.story
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FormatAgeTest {
    @Test fun buckets() {
        assertEquals("just now", formatAge(1_000, 990))
        assertEquals("5m ago", formatAge(1_000, 700))
        assertEquals("2h ago", formatAge(10_000, 2_800))
        assertEquals("3d ago", formatAge(300_000, 40_000))
    }

    @Test fun clockSkewNeverGoesNegative() = assertEquals("just now", formatAge(100, 500))
}

private class FakeStoryRepository(var result: Outcome<List<Story>>) : StoryRepository {
    var calls = 0
    override suspend fun topStories(limit: Int): Outcome<List<Story>> {
        calls++
        return result
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class StoryListPresenterTest {

    private val stories = listOf(
        story(1, title = "Kotlin news", postedAt = 9_400),
        story(2, title = "Swift news", postedAt = 9_000),
        story(3, title = "Rust news", postedAt = 0),
    )

    private fun presenter(
        repo: FakeStoryRepository,
        store: InMemoryKeyValueStore = InMemoryKeyValueStore(),
    ): StoryListPresenter {
        val bookmarks = BookmarkRepositoryImpl(store)
        return StoryListPresenter(
            getTopStories = GetTopStories(repo),
            searchStories = SearchStories(),
            toggleBookmarkUseCase = ToggleBookmark(bookmarks),
            observeBookmarks = ObserveBookmarks(bookmarks),
            nowEpochSeconds = { 10_000 },
            dispatcher = UnconfinedTestDispatcher(),
        )
    }

    @Test fun loadsOnCreationAndFormatsRows() = runTest {
        val p = presenter(FakeStoryRepository(Outcome.Success(stories)))
        val state = p.state.value

        assertEquals(listOf(1L, 2L, 3L), state.items.map { it.id })
        assertEquals(false, state.isLoading)
        assertNull(state.errorMessage)
        assertEquals("example.com", state.items[0].host)
        assertEquals("10 points · 2 comments · 10m ago", state.items[0].meta)
        p.close()
    }

    @Test fun failureShowsMessageAndRefreshRecovers() = runTest {
        val repo = FakeStoryRepository(Outcome.Failure(AppError.Network))
        val p = presenter(repo)
        assertNotNull(p.state.value.errorMessage)
        assertTrue(p.state.value.items.isEmpty())

        repo.result = Outcome.Success(stories)
        p.refresh()

        assertNull(p.state.value.errorMessage)
        assertEquals(3, p.state.value.items.size)
        assertEquals(2, repo.calls)
        p.close()
    }

    @Test fun searchFiltersWithoutAnotherNetworkCall() = runTest {
        val repo = FakeStoryRepository(Outcome.Success(stories))
        val p = presenter(repo)

        p.onQueryChange("swift")

        assertEquals(listOf(2L), p.state.value.items.map { it.id })
        assertEquals(1, repo.calls)
        p.close()
    }

    @Test fun bookmarkingMarksTheRowAndSavedFilterShowsOnlySaved() = runTest {
        val p = presenter(FakeStoryRepository(Outcome.Success(stories)))

        p.toggleBookmark(2)
        assertEquals(listOf(2L), p.state.value.items.filter { it.isBookmarked }.map { it.id })

        p.onSavedOnlyChange(true)
        assertEquals(listOf(2L), p.state.value.items.map { it.id })

        p.toggleBookmark(2) // un-save while in the Saved tab
        assertTrue(p.state.value.items.isEmpty())
        p.close()
    }

    @Test fun savedStoriesStillShowWhenTheServerIsDown() = runTest {
        val store = InMemoryKeyValueStore()
        presenter(FakeStoryRepository(Outcome.Success(stories)), store = store).apply {
            toggleBookmark(1)
            close()
        }

        // "Next launch": offline, so the list fails to load, but the saved tab still works.
        val offline = presenter(FakeStoryRepository(Outcome.Failure(AppError.Network)), store = store)
        offline.onSavedOnlyChange(true)

        assertEquals(listOf(1L), offline.state.value.items.map { it.id })
        assertNotNull(offline.state.value.errorMessage)
        offline.close()
    }

    @Test fun watchDeliversCurrentAndLaterStatesUntilCancelled() = runTest {
        val p = presenter(FakeStoryRepository(Outcome.Success(stories)))
        val seen = mutableListOf<String>()

        val subscription = p.watch { seen += it.query }
        p.onQueryChange("a")
        subscription.cancel()
        p.onQueryChange("b")

        assertEquals(listOf("", "a"), seen)
        p.close()
    }
}
