package com.rakesh1308.hnreader.android

import android.app.Application
import com.rakesh1308.hnreader.AppContainer
import com.rakesh1308.hnreader.data.local.AndroidAppContainerFactory

/** Android's equivalent of iOS's `@main App`: builds the container once for the whole process. */
class HnReaderApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AndroidAppContainerFactory.create(this)
    }
}
