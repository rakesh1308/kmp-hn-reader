package com.rakesh1308.hnreader.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rakesh1308.hnreader.AppContainer

/**
 * Wraps the shared presenter in an androidx ViewModel so it survives rotation,
 * and closes it (cancelling its coroutines) when the screen is finished for good.
 */
class StoryListViewModel(val container: AppContainer) : ViewModel() {
    val presenter = container.createStoryListPresenter()

    override fun onCleared() {
        presenter.close()
    }
}

class MainActivity : ComponentActivity() {

    private val viewModel: StoryListViewModel by viewModels {
        viewModelFactory {
            initializer { StoryListViewModel((application as HnReaderApp).container) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                StoryListScreen(
                    presenter = viewModel.presenter,
                    platform = viewModel.container.platformLabel,
                )
            }
        }
    }
}
