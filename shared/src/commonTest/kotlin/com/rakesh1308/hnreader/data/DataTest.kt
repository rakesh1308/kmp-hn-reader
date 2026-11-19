package com.rakesh1308.hnreader.data

import com.rakesh1308.hnreader.data.local.InMemoryKeyValueStore
import com.rakesh1308.hnreader.data.remote.HnApi
import com.rakesh1308.hnreader.data.remote.createHnHttpClient
import com.rakesh1308.hnreader.data.repository.BookmarkRepositoryImpl
import com.rakesh1308.hnreader.data.repository.StoryRepositoryImpl
import com.rakesh1308.hnreader.domain.model.AppError
import com.rakesh1308.hnreader.domain.model.Outcome
import com.rakesh1308.hnreader.story
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

/** Server interaction tested against a fake server: no network, same code path as production. */
class StoryRepositoryTest {

    private fun repo(handler: (path: String) -> Pair<HttpStatusCode, String>): StoryRepositoryImpl {
        val engine = MockEngine { request ->
            val (status, body) = handler(request.url.encodedPath)
            if (status.value >= 400) respondError(status) else respond(body, status, jsonHeaders)
        }
        return StoryRepositoryImpl(HnApi(createHnHttpClient(engine), baseUrl = "https://fake.test/v0"))
    }

    @Test fun mapsItemsToStoriesInServerOrder() = runTest {
        val repo = repo { path ->
            HttpStatusCode.OK to when (path) {
                "/v0/topstories.json" -> "[30, 10, 20]"
                "/v0/item/30.json" -> """{"id":30,"by":"a","time":300,"title":"Third","url":"https://x.dev/p","score":7,"descendants":3,"type":"story","extra":"ignored"}"""
                "/v0/item/10.json" -> """{"id":10,"by":"b","time":100,"title":"First","score":1,"descendants":0,"type":"story"}"""
                "/v0/item/20.json" -> """{"id":20,"by":"c","time":200,"title":"Second","url":"","score":2,"descendants":1}"""
                else -> error("unexpected $path")
            }
        }

        val result = repo.topStories(limit = 10)

        assertIs<Outcome.Success<*>>(result)
        val stories = (result as Outcome.Success).value
        assertEquals(listOf(30L, 10L, 20L), stories.map { it.id })
        assertEquals("https://x.dev/p", stories[0].url)
        assertEquals(3, stories[0].commentCount)
        assertEquals(null, stories[2].url) // empty url string becomes null
    }

    @Test fun limitsTheNumberOfItemRequests() = runTest {
        val requested = mutableListOf<String>()
        val repo = repo { path ->
            requested += path
            HttpStatusCode.OK to when {
                path.endsWith("topstories.json") -> "[1,2,3,4,5]"
                else -> """{"id":1,"title":"t"}"""
            }
        }
        repo.topStories(limit = 2)
        assertEquals(1 + 2, requested.size)
    }

    @Test fun dropsDeletedDeadAndNullItems() = runTest {
        val repo = repo { path ->
            HttpStatusCode.OK to when (path) {
                "/v0/topstories.json" -> "[1,2,3,4]"
                "/v0/item/1.json" -> """{"id":1,"title":"keep","by":"a"}"""
                "/v0/item/2.json" -> """{"id":2,"deleted":true}"""
                "/v0/item/3.json" -> """{"id":3,"title":"dead one","dead":true}"""
                "/v0/item/4.json" -> "null"
                else -> error("unexpected $path")
            }
        }
        val result = repo.topStories(10) as Outcome.Success
        assertEquals(listOf(1L), result.value.map { it.id })
    }

    @Test fun serverErrorBecomesFailureWithStatusCode() = runTest {
        val result = repo { HttpStatusCode.InternalServerError to "" }.topStories(10)
        assertEquals(Outcome.Failure(AppError.Server(500)), result)
    }

    @Test fun malformedJsonBecomesInvalidResponse() = runTest {
        val result = repo { HttpStatusCode.OK to "{ this is not json" }.topStories(10)
        assertEquals(Outcome.Failure(AppError.InvalidResponse), result)
    }
}

class BookmarkRepositoryTest {
    @Test fun newestSaveComesFirstAndDuplicatesAreIgnored() = runTest {
        val repo = BookmarkRepositoryImpl(InMemoryKeyValueStore())
        repo.add(story(1)); repo.add(story(2)); repo.add(story(1))
        assertEquals(listOf(2L, 1L), repo.observe().first().map { it.id })
    }

    @Test fun survivesRestartBecauseItIsWrittenToTheStore() = runTest {
        val store = InMemoryKeyValueStore()
        BookmarkRepositoryImpl(store).add(story(7, title = "Saved offline", url = null))

        val afterRestart = BookmarkRepositoryImpl(store) // new instance, same disk
        val saved = afterRestart.observe().first()

        assertEquals(1, saved.size)
        assertEquals("Saved offline", saved[0].title)
        assertEquals(null, saved[0].url)
    }

    @Test fun corruptStoredDataIsTreatedAsEmpty() = runTest {
        val store = InMemoryKeyValueStore().apply { putString("bookmarks_v1", "{{{ garbage") }
        val repo = BookmarkRepositoryImpl(store)
        assertTrue(repo.observe().first().isEmpty())
        repo.add(story(1)) // and it keeps working afterwards
        assertEquals(1, repo.observe().first().size)
    }

    @Test fun removeDeletesOnlyThatStory() = runTest {
        val repo = BookmarkRepositoryImpl(InMemoryKeyValueStore())
        repo.add(story(1)); repo.add(story(2))
        repo.remove(1)
        assertEquals(listOf(2L), repo.observe().first().map { it.id })
    }
}
