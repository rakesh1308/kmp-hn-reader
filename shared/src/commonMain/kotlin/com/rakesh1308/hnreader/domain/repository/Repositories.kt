package com.rakesh1308.hnreader.domain.repository

import com.rakesh1308.hnreader.domain.model.Outcome
import com.rakesh1308.hnreader.domain.model.Story
import kotlinx.coroutines.flow.Flow

/**
 * Interfaces are OWNED by the domain and IMPLEMENTED by the data layer.
 * That is the dependency rule: the data layer imports the domain, never the reverse.
 */
interface StoryRepository {
    /** Top stories from the server, best first. */
    suspend fun topStories(limit: Int): Outcome<List<Story>>
}

interface BookmarkRepository {
    /** Emits the saved stories (newest save first) and again after every change. */
    fun observe(): Flow<List<Story>>

    suspend fun contains(id: Long): Boolean
    suspend fun add(story: Story)
    suspend fun remove(id: Long)
}
