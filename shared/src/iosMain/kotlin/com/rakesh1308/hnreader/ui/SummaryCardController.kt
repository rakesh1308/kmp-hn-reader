package com.rakesh1308.hnreader.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * iOS entry point for the shared card. Swift cannot call a @Composable directly, so this class
 * wraps it in a UIViewController (which SwiftUI can host) and exposes update() to push new values.
 *
 * Compose state ([shown], [saved]) lives here; changing it recomposes the card by itself.
 */
class SummaryCardController(private val platform: String) {
    private var shown by mutableIntStateOf(0)
    private var saved by mutableIntStateOf(0)

    val viewController: UIViewController = ComposeUIViewController {
        SummaryCard(shown = shown, saved = saved, platform = platform)
    }

    fun update(shown: Int, saved: Int) {
        this.shown = shown
        this.saved = saved
    }
}
