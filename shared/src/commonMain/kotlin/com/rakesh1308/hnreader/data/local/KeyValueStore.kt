package com.rakesh1308.hnreader.data.local

/**
 * The smallest persistence seam the app needs. Each platform provides its own implementation:
 *  - Android: SharedPreferences  (shared/src/androidMain)
 *  - iOS:     NSUserDefaults     (shared/src/iosMain)
 *  - tests:   [InMemoryKeyValueStore]
 *
 * Production next step: replace with SQLDelight (see README "What to learn next").
 */
interface KeyValueStore {
    suspend fun getString(key: String): String?
    suspend fun putString(key: String, value: String)
}

class InMemoryKeyValueStore : KeyValueStore {
    private val values = mutableMapOf<String, String>()
    override suspend fun getString(key: String): String? = values[key]
    override suspend fun putString(key: String, value: String) {
        values[key] = value
    }
}
