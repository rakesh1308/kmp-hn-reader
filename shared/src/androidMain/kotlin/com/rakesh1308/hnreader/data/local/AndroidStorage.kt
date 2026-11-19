package com.rakesh1308.hnreader.data.local

import android.content.Context
import com.rakesh1308.hnreader.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Android's `actual` for persistence: SharedPreferences, off the main thread. */
class SharedPrefsKeyValueStore(context: Context) : KeyValueStore {
    private val prefs = context.applicationContext
        .getSharedPreferences("hn_reader", Context.MODE_PRIVATE)

    override suspend fun getString(key: String): String? =
        withContext(Dispatchers.IO) { prefs.getString(key, null) }

    override suspend fun putString(key: String, value: String) =
        withContext(Dispatchers.IO) { prefs.edit().putString(key, value).apply() }
}

/** Entry point for the Android app: `AndroidAppContainerFactory.create(context)`. */
object AndroidAppContainerFactory {
    fun create(context: Context): AppContainer = AppContainer(SharedPrefsKeyValueStore(context))
}
