package com.rakesh1308.hnreader.data.repository

import com.rakesh1308.hnreader.data.local.KeyValueStore
import com.rakesh1308.hnreader.data.remote.HnJson
import com.rakesh1308.hnreader.domain.model.Story
import com.rakesh1308.hnreader.domain.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Storage shape for a saved story. Separate from the domain [Story] so the domain stays annotation-free. */
@Serializable
internal data class StoredStory(
    val id: Long,
    val title: String,
    val url: String? = null,
    val author: String,
    val score: Int,
    val commentCount: Int,
    val postedAt: Long,
)

private fun Story.toStored() = StoredStory(id, title, url, author, score, commentCount, postedAtEpochSeconds)
private fun StoredStory.toStory() = Story(id, title, url, author, score, commentCount, postedAt)

/** Saved stories kept in memory (as a StateFlow) and written through to a [KeyValueStore] as JSON. */
internal class BookmarkRepositoryImpl(
    private val store: KeyValueStore,
    private val json: Json = HnJson,
) : BookmarkRepository {

    private val mutex = Mutex()
    private val state = MutableStateFlow<List<Story>?>(null) // null = not loaded from disk yet

    override fun observe(): Flow<List<Story>> = flow {
        load()
        emitAll(state.filterNotNull())
    }

    override suspend fun contains(id: Long): Boolean = load().any { it.id == id }

    override suspend fun add(story: Story) = mutate { list ->
        if (list.any { it.id == story.id }) list else listOf(story) + list
    }

    override suspend fun remove(id: Long) = mutate { list -> list.filterNot { it.id == id } }

    private suspend fun load(): List<Story> =
        state.value ?: mutex.withLock {
            state.value ?: readFromStore().also { state.value = it }
        }

    private suspend fun mutate(change: (List<Story>) -> List<Story>) {
        load()
        mutex.withLock {
            val updated = change(state.value.orEmpty())
            store.putString(KEY, json.encodeToString(updated.map { it.toStored() })) // persist first...
            state.value = updated                                                    // ...then publish
        }
    }

    /** Corrupt or unreadable data is treated as "nothing saved" rather than crashing on launch. */
    private suspend fun readFromStore(): List<Story> {
        val raw = store.getString(KEY) ?: return emptyList()
        return try {
            json.decodeFromString<List<StoredStory>>(raw).map { it.toStory() }
        } catch (e: kotlinx.serialization.SerializationException) {
            emptyList()
        }
    }

    private companion object {
        const val KEY = "bookmarks_v1"
    }
}
