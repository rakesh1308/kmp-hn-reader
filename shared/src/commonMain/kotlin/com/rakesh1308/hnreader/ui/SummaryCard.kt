package com.rakesh1308.hnreader.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The ONE shared screen piece. It is written once in commonMain and drawn by Compose on both
 * platforms: Android hosts it as a normal composable, iOS hosts it inside a UIViewController
 * (see SummaryCardController in iosMain).
 *
 * It is deliberately "dumb": it receives plain values and holds no logic, so it can't drift
 * between platforms. The text is built by [summaryLine], which has a plain unit test.
 */
@Composable
fun SummaryCard(shown: Int, saved: Int, platform: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text("Shared Compose UI · running on $platform", style = MaterialTheme.typography.labelMedium)
            Text(summaryLine(shown, saved), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Pure function: easy to test without any UI. */
internal fun summaryLine(shown: Int, saved: Int): String {
    val stories = if (shown == 1) "1 story" else "$shown stories"
    return "$stories shown · $saved saved"
}
