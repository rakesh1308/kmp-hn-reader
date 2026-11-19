package com.rakesh1308.hnreader.domain

import com.rakesh1308.hnreader.data.local.InMemoryKeyValueStore
import com.rakesh1308.hnreader.data.repository.BookmarkRepositoryImpl
import com.rakesh1308.hnreader.domain.usecase.SearchStories
import com.rakesh1308.hnreader.domain.usecase.ToggleBookmark
import com.rakesh1308.hnreader.story
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StoryHostTest {
    @Test fun stripsSchemeWwwAndPath() =
        assertEquals("example.com", story(1, url = "https://www.example.com/a/b?x=1#frag").host)

    @Test fun keepsSubdomains() =
        assertEquals("blog.example.org", story(1, url = "http://blog.example.org").host)

    @Test fun textPostWithoutUrlHasNoHost() = assertNull(story(1, url = null).host)

    @Test fun garbageUrlHasNoHost() = assertNull(story(1, url = "not a url").host)
}

class SearchStoriesTest {
    private val search = SearchStories()
    private val all = listOf(
        story(1, title = "Kotlin Multiplatform is stable", url = "https://kotlinlang.org/x", author = "jb"),
        story(2, title = "Show HN: A tiny database", url = "https://www.sqlite.org/", author = "dhh"),
        story(3, title = "आपत्कालीन सेवा नकाशा", url = null, author = "pune_dev"), // Devanagari title
    )

    @Test fun blankQueryReturnsEverything() = assertEquals(all, search(all, "   "))

    @Test fun matchesTitleCaseInsensitively() =
        assertEquals(listOf(1L), search(all, "KOTLIN").map { it.id })

    @Test fun matchesHostAndAuthor() {
        assertEquals(listOf(2L), search(all, "sqlite").map { it.id })
        assertEquals(listOf(2L), search(all, "dhh").map { it.id })
    }

    @Test fun allWordsMustMatch() {
        assertEquals(listOf(1L), search(all, "kotlin stable").map { it.id })
        assertEquals(emptyList(), search(all, "kotlin sqlite"))
    }

    @Test fun nonAsciiQueriesWork() =
        assertEquals(listOf(3L), search(all, "नकाशा").map { it.id })
}

class ToggleBookmarkTest {
    private val repo = BookmarkRepositoryImpl(InMemoryKeyValueStore())
    private val toggle = ToggleBookmark(repo)

    @Test fun firstToggleSavesSecondRemoves() = runTest {
        assertEquals(true, toggle(story(1)))
        assertEquals(true, repo.contains(1))
        assertEquals(false, toggle(story(1)))
        assertEquals(false, repo.contains(1))
    }

    @Test fun togglingOneStoryLeavesOthersAlone() = runTest {
        toggle(story(1)); toggle(story(2))
        toggle(story(1))
        assertEquals(listOf(2L), repo.observe().first().map { it.id })
    }
}
