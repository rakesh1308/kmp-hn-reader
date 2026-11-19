package com.rakesh1308.hnreader.platform

actual fun platformName(): String = "JVM ${System.getProperty("java.version")}"
