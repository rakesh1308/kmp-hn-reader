package com.rakesh1308.hnreader.data.repository

import com.rakesh1308.hnreader.data.remote.HnApi
import com.rakesh1308.hnreader.data.remote.toStoryOrNull
import com.rakesh1308.hnreader.domain.model.AppError
import com.rakesh1308.hnreader.domain.model.Outcome
import com.rakesh1308.hnreader.domain.model.Story
import com.rakesh1308.hnreader.domain.repository.StoryRepository
import io.ktor.client.plugins.ResponseException
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

internal class StoryRepositoryImpl(private val api: HnApi) : StoryRepository {

    /**
     * 1 request for the id list, then N parallel requests for the items (the HN API has no bulk endpoint).
     * If any single item request fails, the whole call fails: simple and predictable for a demo.
     */
    override suspend fun topStories(limit: Int): Outcome<List<Story>> = runCatchingOutcome {
        val ids = api.topStoryIds().take(limit)
        coroutineScope { ids.map { id -> async { api.item(id) } }.awaitAll() }
            .mapNotNull { it?.toStoryOrNull() }
    }
}

/** Turns exceptions into [Outcome.Failure]. Cancellation MUST be rethrown or coroutines break. */
internal suspend fun <T> runCatchingOutcome(block: suspend () -> T): Outcome<T> =
    try {
        Outcome.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: ResponseException) {
        Outcome.Failure(AppError.Server(e.response.status.value))
    } catch (e: SerializationException) {
        Outcome.Failure(AppError.InvalidResponse)
    } catch (e: ContentConvertException) {
        Outcome.Failure(AppError.InvalidResponse)
    } catch (e: IOException) {
        Outcome.Failure(AppError.Network)
    } catch (e: Exception) {
        Outcome.Failure(AppError.Unknown(e.message))
    }
