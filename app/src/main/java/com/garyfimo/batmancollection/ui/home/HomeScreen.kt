package com.garyfimo.batmancollection.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import coil3.compose.AsyncImage
import com.garyfimo.batmancollection.data.Figure
import com.garyfimo.batmancollection.data.FigureType
import com.garyfimo.batmancollection.data.canAddFigures
import com.garyfimo.batmancollection.navigation.LocalSharedTransitionScope
import com.garyfimo.batmancollection.navigation.batmanImageSharedKey
import com.garyfimo.batmancollection.navigation.batmanPopupImageKey
import kotlin.math.abs
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    onItemClick: (String) -> Unit,
    onAddFigure: () -> Unit = {},
    onSignIn: () -> Unit = {},
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val canAdd = authState.canAddFigures()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.RemoveFailed -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    var isSearching by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    val query = (state as? HomeUiState.Ready)?.query.orEmpty()
    val currentSort = (state as? HomeUiState.Ready)?.sort ?: HomeSort.CreatedAt
    val searchFocus = remember { FocusRequester() }
    LaunchedEffect(isSearching) {
        if (isSearching) searchFocus.requestFocus()
    }
    val closeSearch: () -> Unit = {
        isSearching = false
        viewModel.setQuery("")
    }
    BackHandler(enabled = isSearching) { closeSearch() }
    // selectedFigure is the live selection; lastSelectedFigure keeps the previously
    // selected one alive during the overlay's exit animation so the shared-element
    // morph has content to animate back from.
    var selectedFigure by remember { mutableStateOf<Figure?>(null) }
    var lastSelectedFigure by remember { mutableStateOf<Figure?>(null) }
    selectedFigure?.let { lastSelectedFigure = it }
    // Gate the thumbnail's popup shared-element modifier: it must be attached while
    // the popup is open AND through the exit animation, then detached so a later
    // detail-nav transition doesn't inherit a stale caller-managed "visible = true"
    // state (which would hold the thumbnail in the shared-transition overlay layer).
    var popupActive by remember { mutableStateOf(false) }
    LaunchedEffect(selectedFigure) {
        if (selectedFigure != null) {
            popupActive = true
        } else if (popupActive) {
            delay(POPUP_FADE_MILLIS.toLong() + POPUP_GRACE_MILLIS)
            popupActive = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        if (isSearching) {
                            TextField(
                                value = query,
                                onValueChange = viewModel::setQuery,
                                placeholder = { Text("Search by name or series") },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                ),
                                trailingIcon = {
                                    if (query.isNotEmpty()) {
                                        IconButton(
                                            onClick = { viewModel.setQuery("") },
                                            modifier = Modifier.testTag(TAG_HOME_SEARCH_CLEAR),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Clear search",
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(searchFocus)
                                    .testTag(TAG_HOME_SEARCH_FIELD),
                            )
                        } else {
                            Text("Batman Collection")
                        }
                    },
                    navigationIcon = {
                        if (isSearching) {
                            IconButton(
                                onClick = closeSearch,
                                modifier = Modifier.testTag(TAG_HOME_SEARCH_CLOSE),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Close search",
                                )
                            }
                        }
                    },
                    actions = {
                        if (!isSearching) {
                            IconButton(
                                onClick = { isSearching = true },
                                modifier = Modifier.testTag(TAG_HOME_SEARCH_OPEN),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = "Search",
                                )
                            }
                            Box {
                                TextButton(
                                    onClick = { sortMenuOpen = true },
                                    modifier = Modifier.testTag(TAG_HOME_SORT_OPEN),
                                ) { Text("Sort") }
                                DropdownMenu(
                                    expanded = sortMenuOpen,
                                    onDismissRequest = { sortMenuOpen = false },
                                    modifier = Modifier.testTag(TAG_HOME_SORT_MENU),
                                ) {
                                    HomeSort.entries.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option.label) },
                                            onClick = {
                                                viewModel.setSort(option)
                                                sortMenuOpen = false
                                            },
                                            leadingIcon = {
                                                if (option == currentSort) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Check,
                                                        contentDescription = null,
                                                    )
                                                }
                                            },
                                            modifier = Modifier.testTag(
                                                "$TAG_HOME_SORT_ITEM_PREFIX${option.name}",
                                            ),
                                        )
                                    }
                                }
                            }
                            if (authState is com.garyfimo.batmancollection.data.AuthState.SignedIn) {
                                TextButton(
                                    onClick = viewModel::signOut,
                                    modifier = Modifier.testTag(TAG_HOME_SIGN_OUT),
                                ) { Text("Sign out") }
                            } else {
                                TextButton(
                                    onClick = onSignIn,
                                    modifier = Modifier.testTag(TAG_HOME_SIGN_IN),
                                ) { Text("Sign in") }
                            }
                        }
                    },
                )
            },
            floatingActionButton = {
                if (canAdd) {
                    FloatingActionButton(
                        onClick = onAddFigure,
                        modifier = Modifier.testTag(TAG_HOME_ADD_FAB),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add figure")
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    is HomeUiState.Ready -> Column(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        FilterRow(
                            selected = s.filter,
                            onSelected = viewModel::setFilter,
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag(TAG_HOME_LIST),
                            verticalArrangement = Arrangement.Top,
                        ) {
                            items(s.visibleItems, key = { it.id }) { figure ->
                                SwipeToConfirmDelete(
                                    key = figure.id,
                                    enabled = canAdd,
                                    onConfirmDelete = { viewModel.remove(figure.id) },
                                ) {
                                    FigureRow(
                                        figure = figure,
                                        isExpanded = selectedFigure?.id == figure.id,
                                        popupActive = popupActive,
                                        onRowClick = { onItemClick(figure.id) },
                                        onImageClick = { selectedFigure = figure },
                                    )
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = selectedFigure != null,
            enter = fadeIn(animationSpec = tween(POPUP_FADE_MILLIS)),
            exit = fadeOut(animationSpec = tween(POPUP_FADE_MILLIS)),
            modifier = Modifier.fillMaxSize(),
        ) {
            val figure = lastSelectedFigure ?: return@AnimatedVisibility
            BackHandler(enabled = selectedFigure != null) { selectedFigure = null }
            ExpandedImageOverlay(
                figure = figure,
                visibleAsTarget = selectedFigure?.id == figure.id,
                onDismiss = { selectedFigure = null },
            )
        }
    }
}

@Composable
private fun FilterRow(
    selected: FigureType?,
    onSelected: (FigureType?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(TAG_HOME_FILTER_ROW),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelected(null) },
            label = { Text("All") },
            modifier = Modifier.testTag(TAG_HOME_FILTER_ALL),
        )
        FigureType.entries.forEach { tipo ->
            FilterChip(
                selected = selected == tipo,
                onClick = { onSelected(tipo) },
                label = { Text(tipo.label) },
                modifier = Modifier.testTag("$TAG_HOME_FILTER_PREFIX${tipo.name}"),
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun FigureRow(
    figure: Figure,
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
                sharedContentState = rememberSharedContentState(batmanImageSharedKey(figure.id)),
                animatedVisibilityScope = LocalNavAnimatedContentScope.current,
            )
            if (popupActive) {
                navScoped.sharedElementWithCallerManagedVisibility(
                    sharedContentState = rememberSharedContentState(batmanPopupImageKey(figure.id)),
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
                model = figure.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = thumbnailModifier
                    .clickable(onClick = onImageClick)
                    .testTag("$TAG_HOME_THUMB_PREFIX${figure.id}"),
            )
        },
        overlineContent = { Text(figure.tipo.label) },
        headlineContent = { Text(figure.nombre) },
        supportingContent = { Text(figure.serie) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRowClick)
            .testTag("$TAG_HOME_ITEM_PREFIX${figure.id}"),
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ExpandedImageOverlay(
    figure: Figure,
    visibleAsTarget: Boolean,
    onDismiss: () -> Unit,
) {
    val sharedScope = LocalSharedTransitionScope.current
    // aspectRatio(1f) gives the hero a deterministic size even before Coil resolves
    // the image, keeps tall / wide pictures fully visible under ContentScale.Fit, and
    // leaves the surrounding scrim large enough to stay tap-to-dismiss.
    val base = Modifier
        .fillMaxWidth()
        .padding(24.dp)
        .aspectRatio(1f)
        .clip(RoundedCornerShape(16.dp))
    val imageModifier = if (sharedScope != null) {
        with(sharedScope) {
            base.sharedElementWithCallerManagedVisibility(
                sharedContentState = rememberSharedContentState(batmanPopupImageKey(figure.id)),
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
            model = figure.imageUrl,
            contentDescription = "${figure.nombre} — ${figure.tipo.label}",
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

@Composable
private fun SwipeToConfirmDelete(
    key: Any,
    enabled: Boolean,
    onConfirmDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        content()
        return
    }
    val density = LocalDensity.current
    val revealedPx = with(density) { CONFIRM_BUTTON_WIDTH.toPx() }
    val offsetX = remember(key) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val isRevealed = offsetX.value <= -revealedPx + REVEAL_SNAP_TOLERANCE_PX

    Box(modifier = Modifier.fillMaxWidth()) {
        // Behind the content: the confirm button, revealed as the content slides left.
        // matchParentSize sizes the overlay to the content so the button height tracks
        // the row's natural height instead of forcing a fixed one.
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
                    .testTag("$TAG_HOME_DELETE_PREFIX$key"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Confirm delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
        // Foreground: the row content, translated horizontally by the drag.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.toInt(), 0) }
                .background(MaterialTheme.colorScheme.surface)
                .pointerInput(key, revealedPx) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            // Snap to the nearer anchor based on how far the row was pulled.
                            val target = if (abs(offsetX.value) > revealedPx / 2f) -revealedPx else 0f
                            scope.launch { offsetX.animateTo(target) }
                        },
                        onDragCancel = {
                            scope.launch { offsetX.animateTo(0f) }
                        },
                    ) { _, dragAmount ->
                        // Only allow leftward reveal; clamp so the row can't drift past the button.
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

// Spring duration scales as 1/sqrt(stiffness). StiffnessMediumLow (400f) gave the
// previous feel; stiffness/1.3^2 ≈ 237f makes the morph ~30% slower.
private fun popupBoundsSpring() = spring<androidx.compose.ui.geometry.Rect>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = POPUP_BOUNDS_STIFFNESS,
)

private const val POPUP_BOUNDS_STIFFNESS = 237f
private const val POPUP_FADE_MILLIS = 286
private const val POPUP_GRACE_MILLIS = 100L

const val TAG_HOME_ADD_FAB = "home.add"
const val TAG_HOME_SIGN_IN = "home.sign_in"
const val TAG_HOME_SIGN_OUT = "home.sign_out"
const val TAG_HOME_ROOT = "home.root"
const val TAG_HOME_LOADING = "home.loading"
const val TAG_HOME_ERROR = "home.error"
const val TAG_HOME_LIST = "home.list"
const val TAG_HOME_FILTER_ROW = "home.filter.row"
const val TAG_HOME_FILTER_ALL = "home.filter.all"
const val TAG_HOME_FILTER_PREFIX = "home.filter."
const val TAG_HOME_ITEM_PREFIX = "home.item."
const val TAG_HOME_THUMB_PREFIX = "home.thumb."
const val TAG_HOME_DELETE_PREFIX = "home.delete."
const val TAG_HOME_SEARCH_OPEN = "home.search.open"
const val TAG_HOME_SEARCH_CLOSE = "home.search.close"
const val TAG_HOME_SEARCH_CLEAR = "home.search.clear"
const val TAG_HOME_SEARCH_FIELD = "home.search.field"
const val TAG_HOME_SORT_OPEN = "home.sort.open"
const val TAG_HOME_SORT_MENU = "home.sort.menu"
const val TAG_HOME_SORT_ITEM_PREFIX = "home.sort.item."
const val TAG_HOME_OVERLAY = "home.overlay"
const val TAG_HOME_OVERLAY_IMAGE = "home.overlay.image"
