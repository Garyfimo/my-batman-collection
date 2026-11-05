package com.garyfimo.batmancollection.figure.presentation.details

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import coil3.compose.AsyncImage
import com.garyfimo.batmancollection.figure.domain.Figure
import com.garyfimo.batmancollection.figure.presentation.LocalSharedTransitionScope
import com.garyfimo.batmancollection.figure.presentation.batmanImageSharedKey
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    id: String,
    onBack: () -> Unit,
    viewModel: DetailsViewModel = koinViewModel(key = id) { parametersOf(id) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title(state)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag(TAG_DETAILS_BACK),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
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
                    text = "No figure with id \"$id\".",
                    modifier = Modifier.testTag(TAG_DETAILS_NOT_FOUND),
                )
                is DetailsUiState.Error -> Text(
                    text = s.message,
                    modifier = Modifier.testTag(TAG_DETAILS_ERROR),
                )
                is DetailsUiState.Ready -> FigureDetails(s.figure)
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun FigureDetails(figure: Figure) {
    val sharedScope = LocalSharedTransitionScope.current
    val baseImageModifier = Modifier
        .fillMaxWidth()
        .aspectRatio(4f / 3f)
        .clip(RoundedCornerShape(16.dp))
    val imageModifier = if (sharedScope != null) {
        with(sharedScope) {
            baseImageModifier.sharedElement(
                sharedContentState = rememberSharedContentState(batmanImageSharedKey(figure.id)),
                animatedVisibilityScope = LocalNavAnimatedContentScope.current,
            )
        }
    } else {
        baseImageModifier
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag(TAG_DETAILS_CONTENT),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = figure.imageUrl,
            contentDescription = "${figure.nombre} — ${figure.tipo.label}",
            contentScale = ContentScale.Crop,
            modifier = imageModifier.testTag(TAG_DETAILS_IMAGE),
        )
        Text(figure.nombre, style = MaterialTheme.typography.headlineMedium)
        LabeledLine("Tipo", figure.tipo.label)
        LabeledLine("Serie", figure.serie)
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
    is DetailsUiState.Ready -> state.figure.nombre
    else -> "Details"
}

const val TAG_DETAILS_ROOT = "details.root"
const val TAG_DETAILS_LOADING = "details.loading"
const val TAG_DETAILS_ERROR = "details.error"
const val TAG_DETAILS_NOT_FOUND = "details.notFound"
const val TAG_DETAILS_CONTENT = "details.content"
const val TAG_DETAILS_IMAGE = "details.image"
const val TAG_DETAILS_BACK = "details.back"
