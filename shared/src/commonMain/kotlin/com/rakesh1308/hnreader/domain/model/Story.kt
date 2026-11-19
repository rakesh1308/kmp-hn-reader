package com.rakesh1308.hnreader.domain.model

/**
 * A Hacker News story. This is a *domain* model: plain Kotlin, no serialization or
 * database annotations. The data layer has its own DTO/storage types and maps to this one.
 */
data class Story(
    val id: Long,
    val title: String,
    val url: String?,
    val author: String,
    val score: Int,
    val commentCount: Int,
    val postedAtEpochSeconds: Long,
) {
    /** "example.com" for "https://www.example.com/a/b?x=1"; null for text posts that have no URL. */
    val host: String?
        get() = url
            ?.substringAfter("://", missingDelimiterValue = "")
            ?.substringBefore('/')
            ?.substringBefore('?')
            ?.substringBefore('#')
            ?.removePrefix("www.")
            ?.takeIf { it.isNotBlank() }
}
