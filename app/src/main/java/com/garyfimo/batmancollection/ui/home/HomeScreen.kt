package com.garyfimo.batmancollection.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import coil3.compose.AsyncImage
import com.garyfimo.batmancollection.data.Batman
import com.garyfimo.batmancollection.navigation.LocalSharedTransitionScope
import com.garyfimo.batmancollection.navigation.batmanImageSharedKey
import com.garyfimo.batmancollection.navigation.batmanPopupImageKey
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    onItemClick: (String) -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // selectedBatman is the live selection; lastSelectedBatman keeps the previously
    // selected one alive during the overlay's exit animation so the shared-element
    // morph has content to animate back from.
    var selectedBatman by remember { mutableStateOf<Batman?>(null) }
    var lastSelectedBatman by remember { mutableStateOf<Batman?>(null) }
    selectedBatman?.let { lastSelectedBatman = it }
    // Gate the thumbnail's popup shared-element modifier: it must be attached while
    // the popup is open AND through the exit animation, then detached so a later
    // detail-nav transition doesn't inherit a stale caller-managed "visible = true"
    // state (which would hold the thumbnail in the shared-transition overlay layer).
    var popupActive by remember { mutableStateOf(false) }
    LaunchedEffect(selectedBatman) {
        if (selectedBatman != null) {
            popupActive = true
        } else if (popupActive) {
            delay(POPUP_FADE_MILLIS.toLong() + POPUP_GRACE_MILLIS)
            popupActive = false
        }
    }

    Box(Modifier.fillMaxSize()) {
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
                    HomeUiState.Loading -> CircularProgressIndicator(
                        Modifier.testTag(TAG_HOME_LOADING),
                    )
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
                            BatmanRow(
                                batman = batman,
                                isExpanded = selectedBatman?.id == batman.id,
                                popupActive = popupActive,
                                onRowClick = { onItemClick(batman.id) },
                                onImageClick = { selectedBatman = batman },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = selectedBatman != null,
            enter = fadeIn(animationSpec = tween(POPUP_FADE_MILLIS)),
            exit = fadeOut(animationSpec = tween(POPUP_FADE_MILLIS)),
            modifier = Modifier.fillMaxSize(),
        ) {
            val batman = lastSelectedBatman ?: return@AnimatedVisibility
            BackHandler(enabled = selectedBatman != null) { selectedBatman = null }
            ExpandedImageOverlay(
                batman = batman,
                visibleAsTarget = selectedBatman?.id == batman.id,
                onDismiss = { selectedBatman = null },
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun BatmanRow(
    batman: Batman,
    isExpanded: Boolean,
    popupActive: Boolean,
    onRowClick: () -> Unit,
    onImageClick: () -> Unit,
) {
    val sharedScope = LocalSharedTransitionScope.current
    val thumbnailBase = Modifier
        .size(56.dp)
        .clip(RoundedCornerShape(8.dp))
    val thumbnailModifier = if (sharedScope != null) {
        with(sharedScope) {
            val navScoped = thumbnailBase.sharedElement(
                sharedContentState = rememberSharedContentState(batmanImageSharedKey(batman.id)),
                animatedVisibilityScope = LocalNavAnimatedContentScope.current,
            )
            if (popupActive) {
                navScoped.sharedElementWithCallerManagedVisibility(
                    sharedContentState = rememberSharedContentState(batmanPopupImageKey(batman.id)),
                    visible = !isExpanded,
                    boundsTransform = { _, _ -> popupBoundsSpring() },
                )
            } else {
                navScoped
            }
        }
    } else {
        thumbnailBase
    }

    ListItem(
        leadingContent = {
            AsyncImage(
                model = batman.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = thumbnailModifier
                    .clickable(onClick = onImageClick)
                    .testTag("$TAG_HOME_THUMB_PREFIX${batman.id}"),
            )
        },
        overlineContent = { Text(batman.era) },
        headlineContent = { Text(batman.name) },
        supportingContent = { Text(batman.alterEgo) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRowClick)
            .testTag("$TAG_HOME_ITEM_PREFIX${batman.id}"),
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ExpandedImageOverlay(
    batman: Batman,
    visibleAsTarget: Boolean,
    onDismiss: () -> Unit,
) {
    val sharedScope = LocalSharedTransitionScope.current
    val base = Modifier
        .fillMaxWidth()
        .padding(24.dp)
        .clip(RoundedCornerShape(16.dp))
    val imageModifier = if (sharedScope != null) {
        with(sharedScope) {
            base.sharedElementWithCallerManagedVisibility(
                sharedContentState = rememberSharedContentState(batmanPopupImageKey(batman.id)),
                visible = visibleAsTarget,
                boundsTransform = { _, _ -> popupBoundsSpring() },
            )
        }
    } else {
        base
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            )
            .testTag(TAG_HOME_OVERLAY),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = batman.imageUrl,
            contentDescription = "${batman.name} — ${batman.alterEgo}",
            contentScale = ContentScale.Fit,
            modifier = imageModifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .testTag(TAG_HOME_OVERLAY_IMAGE),
        )
    }
}

// Spring duration scales as 1/sqrt(stiffness). StiffnessMediumLow (400f) gave the
// previous feel; stiffness/1.3^2 ≈ 237f makes the morph ~30% slower.
private fun popupBoundsSpring() = spring<androidx.compose.ui.geometry.Rect>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = POPUP_BOUNDS_STIFFNESS,
)

private const val POPUP_BOUNDS_STIFFNESS = 237f
private const val POPUP_FADE_MILLIS = 286
private const val POPUP_GRACE_MILLIS = 100L

const val TAG_HOME_ROOT = "home.root"
const val TAG_HOME_LOADING = "home.loading"
const val TAG_HOME_ERROR = "home.error"
const val TAG_HOME_LIST = "home.list"
const val TAG_HOME_ITEM_PREFIX = "home.item."
const val TAG_HOME_THUMB_PREFIX = "home.thumb."
const val TAG_HOME_OVERLAY = "home.overlay"
const val TAG_HOME_OVERLAY_IMAGE = "home.overlay.image"
