package com.garyfimo.batmancollection.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onItemClick: (String) -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Batman Collection") }) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(TAG_HOME_ROOT),
            contentAlignment = Alignment.Center,
        ) {
            when (val s = state) {
                HomeUiState.Loading -> CircularProgressIndicator(Modifier.testTag(TAG_HOME_LOADING))
                is HomeUiState.Error -> Text(
                    text = s.message,
                    modifier = Modifier.testTag(TAG_HOME_ERROR),
                )
                is HomeUiState.Ready -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(TAG_HOME_LIST),
                    verticalArrangement = Arrangement.Top,
                ) {
                    items(s.items, key = { it.id }) { batman ->
                        ListItem(
                            headlineContent = { Text(batman.name) },
                            supportingContent = { Text(batman.description) },
                            modifier = Modifier.testTag("$TAG_HOME_ITEM_PREFIX${batman.id}"),
                        )
                    }
                }
            }
        }
    }
}

const val TAG_HOME_ROOT = "home.root"
const val TAG_HOME_LOADING = "home.loading"
const val TAG_HOME_ERROR = "home.error"
const val TAG_HOME_LIST = "home.list"
const val TAG_HOME_ITEM_PREFIX = "home.item."
