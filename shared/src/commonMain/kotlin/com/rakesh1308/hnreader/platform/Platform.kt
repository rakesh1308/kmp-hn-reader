package com.rakesh1308.hnreader.platform

/**
 * expect/actual in its simplest form: common code declares WHAT it needs,
 * each platform source set supplies HOW. The compiler checks that every target has an `actual`.
 */
expect fun platformName(): String
