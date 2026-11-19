package com.rakesh1308.hnreader.domain.usecase

import com.rakesh1308.hnreader.domain.model.Outcome
import com.rakesh1308.hnreader.domain.model.Story
import com.rakesh1308.hnreader.domain.repository.BookmarkRepository
import com.rakesh1308.hnreader.domain.repository.StoryRepository
import kotlinx.coroutines.flow.Flow

/** Use case 1: load the front page from the server. */
class GetTopStories(private val stories: StoryRepository) {
    suspend operator fun invoke(limit: Int = DEFAULT_LIMIT): Outcome<List<Story>> =
        stories.topStories(limit.coerceIn(1, MAX_LIMIT))

    companion object {
        const val DEFAULT_LIMIT = 30
        const val MAX_LIMIT = 100
    }
}

/**
 * Use case 2: filter a list by a free-text query. Pure function, no I/O.
 * Every word must match somewhere in title, site or author (case-insensitive).
 */
class SearchStories {
    operator fun invoke(stories: List<Story>, query: String): List<Story> {
        val terms = query.trim().lowercase().split(' ').filter { it.isNotEmpty() }
        if (terms.isEmpty()) return stories
        return stories.filter { story ->
            val haystack = "${story.title} ${story.host.orEmpty()} ${story.author}".lowercase()
            terms.all { it in haystack }
        }
    }
}

/** Use case 3: save a story if it is not saved yet, otherwise un-save it. */
class ToggleBookmark(private val bookmarks: BookmarkRepository) {
    /** Returns true if the story is saved after the call, false if it was removed. */
    suspend operator fun invoke(story: Story): Boolean {
        return if (bookmarks.contains(story.id)) {
            bookmarks.remove(story.id)
            false
        } else {
            bookmarks.add(story)
            true
        }
    }
}

/** Use case 4: stream the saved stories (works offline). */
class ObserveBookmarks(private val bookmarks: BookmarkRepository) {
    operator fun invoke(): Flow<List<Story>> = bookmarks.observe()
}
