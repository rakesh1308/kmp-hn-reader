package com.rakesh1308.hnreader.android

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rakesh1308.hnreader.presentation.StoryItem
import com.rakesh1308.hnreader.presentation.StoryListPresenter
import com.rakesh1308.hnreader.ui.SummaryCard

/**
 * Pure drawing: every decision (filtering, formatting, error text) already happened in the
 * shared presenter. The SwiftUI screen on iOS draws the very same StoryListState.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryListScreen(presenter: StoryListPresenter, platform: String) {
    // StateFlow -> Compose State. Every new StoryListState triggers a redraw of what changed.
    val state by presenter.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HN Reader") },
                actions = { TextButton(onClick = presenter::refresh) { Text("Refresh") } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = presenter::onQueryChange,
                label = { Text("Search title, site or author") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                FilterChip(
                    selected = !state.savedOnly,
                    onClick = { presenter.onSavedOnlyChange(false) },
                    label = { Text("Top") },
                )
                FilterChip(
                    selected = state.savedOnly,
                    onClick = { presenter.onSavedOnlyChange(true) },
                    label = { Text("Saved") },
                )
            }
            if (state.isLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
            }
            state.errorMessage?.let { message ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                    TextButton(onClick = presenter::refresh) { Text("Retry") }
                }
            }
            LazyColumn(Modifier.weight(1f)) {
                items(state.items, key = { it.id }) { item ->
                    StoryRow(item, onToggle = { presenter.toggleBookmark(item.id) })
                    HorizontalDivider()
                }
            }
            // The one SHARED composable (written in commonMain, also drawn on iOS).
            SummaryCard(
                shown = state.items.size,
                saved = state.items.count { it.isBookmarked },
                platform = platform,
            )
        }
    }
}

@Composable
private fun StoryRow(item: StoryItem, onToggle: () -> Unit) {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
                } catch (_: ActivityNotFoundException) {
                    // no browser installed: nothing sensible to do in a demo
                }
            }
            .padding(start = 16.dp),
    ) {
        Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleSmall)
            Text(
                "${item.host} · ${item.meta}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onToggle) { Text(if (item.isBookmarked) "★" else "☆") }
    }
}
