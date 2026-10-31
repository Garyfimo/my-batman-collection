package com.garyfimo.batmancollection.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.garyfimo.batmancollection.data.Batman
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    id: String,
    onBack: () -> Unit,
    viewModel: DetailsViewModel = koinViewModel { parametersOf(id) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title(state)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(TAG_DETAILS_ROOT),
            contentAlignment = Alignment.Center,
        ) {
            when (val s = state) {
                DetailsUiState.Loading -> CircularProgressIndicator(
                    Modifier.testTag(TAG_DETAILS_LOADING),
                )
                DetailsUiState.NotFound -> Text(
                    text = "No Batman with id \"$id\".",
                    modifier = Modifier.testTag(TAG_DETAILS_NOT_FOUND),
                )
                is DetailsUiState.Error -> Text(
                    text = s.message,
                    modifier = Modifier.testTag(TAG_DETAILS_ERROR),
                )
                is DetailsUiState.Ready -> BatmanDetails(s.batman)
            }
        }
    }
}

@Composable
private fun BatmanDetails(batman: Batman) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag(TAG_DETAILS_CONTENT),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(batman.name, style = MaterialTheme.typography.headlineMedium)
        LabeledLine("Alter ego", batman.alterEgo)
        LabeledLine("Era", batman.era)
        LabeledLine("First appearance", batman.firstAppearance)
        Text(
            text = batman.description,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun LabeledLine(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun title(state: DetailsUiState): String = when (state) {
    is DetailsUiState.Ready -> state.batman.name
    else -> "Details"
}

const val TAG_DETAILS_ROOT = "details.root"
const val TAG_DETAILS_LOADING = "details.loading"
const val TAG_DETAILS_ERROR = "details.error"
const val TAG_DETAILS_NOT_FOUND = "details.notFound"
const val TAG_DETAILS_CONTENT = "details.content"
