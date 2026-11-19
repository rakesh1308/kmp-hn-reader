package com.rakesh1308.hnreader.platform

actual fun platformName(): String = "Android ${android.os.Build.VERSION.SDK_INT}"
