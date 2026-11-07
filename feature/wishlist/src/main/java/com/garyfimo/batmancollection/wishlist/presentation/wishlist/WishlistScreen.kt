package com.garyfimo.batmancollection.wishlist.presentation.wishlist

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.garyfimo.batmancollection.wishlist.domain.WishItem
import com.garyfimo.batmancollection.wishlist.domain.canAddWishes
import kotlin.math.abs
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(
    onBack: () -> Unit,
    onAddWish: () -> Unit,
    viewModel: WishlistViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val canAdd = authState.canAddWishes()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is WishlistEffect.OpenUrl -> {
                    val intent = Intent(Intent.ACTION_VIEW, effect.url.toUri())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(intent) }
                        .onFailure { snackbarHostState.showSnackbar("Can't open link") }
                }
                is WishlistEffect.RemoveFailed -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wish list") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag(TAG_WISH_BACK)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (canAdd) {
                FloatingActionButton(
                    onClick = onAddWish,
                    modifier = Modifier.testTag(TAG_WISH_ADD_FAB),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add wish")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(TAG_WISH_ROOT),
            contentAlignment = Alignment.Center,
        ) {
            when (val s = state) {
                WishlistUiState.Loading -> CircularProgressIndicator(
                    Modifier.testTag(TAG_WISH_LOADING),
                )
                is WishlistUiState.Error -> Text(
                    text = s.message,
                    modifier = Modifier.testTag(TAG_WISH_ERROR),
                )
                is WishlistUiState.Ready -> {
                    if (s.items.isEmpty()) {
                        Text(
                            text = "Your wish list is empty",
                            modifier = Modifier.testTag(TAG_WISH_EMPTY),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag(TAG_WISH_LIST),
                            verticalArrangement = Arrangement.Top,
                        ) {
                            items(s.items, key = { it.id }) { wish ->
                                SwipeToConfirmDelete(
                                    key = wish.id,
                                    onConfirmDelete = {
                                        viewModel.onIntent(WishlistIntent.WishRemoved(wish))
                                    },
                                ) {
                                    WishRow(
                                        wish = wish,
                                        onClick = {
                                            viewModel.onIntent(WishlistIntent.WishSelected(wish))
                                        },
                                    )
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WishRow(wish: WishItem, onClick: () -> Unit) {
    ListItem(
        overlineContent = { Text(wish.tipo.label) },
        headlineContent = { Text(wish.nombre) },
        supportingContent = { Text(wish.serie) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("$TAG_WISH_ITEM_PREFIX${wish.id}"),
    )
}

@Composable
private fun SwipeToConfirmDelete(
    key: Any,
    onConfirmDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val revealedPx = with(density) { CONFIRM_BUTTON_WIDTH.toPx() }
    val offsetX = remember(key) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val isRevealed = offsetX.value <= -revealedPx + REVEAL_SNAP_TOLERANCE_PX

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.CenterEnd,
        ) {
            TextButton(
                onClick = {
                    onConfirmDelete()
                    scope.launch { offsetX.animateTo(0f) }
                },
                enabled = isRevealed,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(CONFIRM_BUTTON_WIDTH)
                    .testTag("$TAG_WISH_DELETE_PREFIX$key"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Confirm delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.toInt(), 0) }
                .background(MaterialTheme.colorScheme.surface)
                .pointerInput(key, revealedPx) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val target = if (abs(offsetX.value) > revealedPx / 2f) -revealedPx else 0f
                            scope.launch { offsetX.animateTo(target) }
                        },
                        onDragCancel = {
                            scope.launch { offsetX.animateTo(0f) }
                        },
                    ) { _, dragAmount ->
                        val next = (offsetX.value + dragAmount).coerceIn(-revealedPx, 0f)
                        scope.launch { offsetX.snapTo(next) }
                    }
                },
        ) {
            content()
        }
    }
}

private val CONFIRM_BUTTON_WIDTH: Dp = 96.dp
private const val REVEAL_SNAP_TOLERANCE_PX = 0.5f

const val TAG_WISH_ROOT = "wish.root"
const val TAG_WISH_BACK = "wish.back"
const val TAG_WISH_ADD_FAB = "wish.add"
const val TAG_WISH_LIST = "wish.list"
const val TAG_WISH_LOADING = "wish.loading"
const val TAG_WISH_ERROR = "wish.error"
const val TAG_WISH_EMPTY = "wish.empty"
const val TAG_WISH_ITEM_PREFIX = "wish.item."
const val TAG_WISH_DELETE_PREFIX = "wish.delete."
