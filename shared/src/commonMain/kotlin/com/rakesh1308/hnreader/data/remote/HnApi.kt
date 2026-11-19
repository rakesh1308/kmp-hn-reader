package com.rakesh1308.hnreader.data.remote

import com.rakesh1308.hnreader.domain.model.Story
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal val HnJson = Json {
    ignoreUnknownKeys = true // the server may add fields; we only read what we need
}

/**
 * Builds the HTTP client. With no [engine], Ktor picks the one on the platform's classpath:
 * OkHttp on Android/JVM, Darwin (URLSession) on iOS. Tests pass a MockEngine instead.
 */
internal fun createHnHttpClient(engine: HttpClientEngine? = null): HttpClient {
    val config: HttpClientConfig<*>.() -> Unit = {
        install(ContentNegotiation) { json(HnJson) }
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 20_000
        }
        expectSuccess = true // non-2xx responses throw ResponseException, mapped in the repository
    }
    return if (engine == null) HttpClient(config) else HttpClient(engine, config)
}

/** Wire format of one Hacker News item. DTO = shaped like the server's JSON, not like our domain. */
@Serializable
internal data class ItemDto(
    val id: Long,
    val type: String? = null,
    val by: String? = null,
    val time: Long = 0,
    val title: String? = null,
    val url: String? = null,
    val score: Int = 0,
    val descendants: Int = 0,
    val deleted: Boolean = false,
    val dead: Boolean = false,
)

/** Deleted/dead items and items without a title are not stories we can show. */
internal fun ItemDto.toStoryOrNull(): Story? {
    if (deleted || dead || title.isNullOrBlank()) return null
    return Story(
        id = id,
        title = title,
        url = url?.takeIf { it.isNotBlank() },
        author = by ?: "unknown",
        score = score,
        commentCount = descendants,
        postedAtEpochSeconds = time,
    )
}

/** The two endpoints we use from https://github.com/HackerNews/API */
internal class HnApi(
    private val client: HttpClient,
    private val baseUrl: String = DEFAULT_BASE_URL,
) {
    suspend fun topStoryIds(): List<Long> = client.get("$baseUrl/topstories.json").body()

    /** The server answers a literal `null` for unknown ids, hence the nullable result. */
    suspend fun item(id: Long): ItemDto? = client.get("$baseUrl/item/$id.json").body()

    companion object {
        const val DEFAULT_BASE_URL = "https://hacker-news.firebaseio.com/v0"
    }
}
