package com.rakesh1308.hnreader.data.local

import com.rakesh1308.hnreader.AppContainer
import platform.Foundation.NSUserDefaults

/** iOS's `actual` for persistence: NSUserDefaults. */
class UserDefaultsKeyValueStore(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : KeyValueStore {
    override suspend fun getString(key: String): String? = defaults.stringForKey(key)

    override suspend fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }
}

/**
 * Entry point for the iOS app. Kotlin `object` -> Swift: `IosAppContainerFactory.shared.create()`.
 * (Swift cannot call a Kotlin top-level function directly by name; it would be `FileNameKt.fn()`.)
 */
object IosAppContainerFactory {
    fun create(): AppContainer = AppContainer(UserDefaultsKeyValueStore())
}
