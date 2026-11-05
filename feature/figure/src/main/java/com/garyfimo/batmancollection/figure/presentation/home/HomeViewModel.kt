package com.garyfimo.batmancollection.figure.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.garyfimo.batmancollection.analytics.domain.Analytics
import com.garyfimo.batmancollection.figure.domain.Figure
import com.garyfimo.batmancollection.figure.domain.FigureListResult
import com.garyfimo.batmancollection.figure.domain.FigureRemoveResult
import com.garyfimo.batmancollection.figure.domain.FigureRepository
import com.garyfimo.batmancollection.login.domain.AuthRepository
import com.garyfimo.batmancollection.login.domain.AuthState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: FigureRepository,
    private val authRepository: AuthRepository,
    analytics: Analytics,
) : ViewModel() {

    private val analyticEvents = HomeAnalyticEvents(analytics)

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    val authState: StateFlow<AuthState> = authRepository.state

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        analyticEvents.onScreenView()
        viewModelScope.launch {
            repository.observe().collect { result ->
                val previous = _state.value as? HomeUiState.Ready
                _state.value = when (result) {
                    is FigureListResult.Success -> HomeUiState.Ready(
                        items = result.items,
                        filter = previous?.filter,
                        query = previous?.query.orEmpty(),
                        sort = previous?.sort ?: HomeSort.CreatedAt,
                    )
                    is FigureListResult.Failure ->
                        HomeUiState.Error(result.cause.message ?: "Unknown error")
                }
            }
        }
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.FilterChanged -> {
                updateReady { it.copy(filter = intent.filter) }
                if (intent.filter == null) analyticEvents.onFilterCleared()
                else analyticEvents.onFilterApplied(intent.filter)
            }
            is HomeIntent.QueryChanged -> updateReady { it.copy(query = intent.query) }
            is HomeIntent.SortChanged -> {
                updateReady { it.copy(sort = intent.sort) }
                analyticEvents.onSortChanged(intent.sort)
            }
            is HomeIntent.FigureSelected -> analyticEvents.onFigureSelected(intent.figure)
            is HomeIntent.FigureRemoved -> remove(intent.figure)
            HomeIntent.SearchOpened -> analyticEvents.onSearchOpened()
            HomeIntent.SearchClosed -> {
                val term = (_state.value as? HomeUiState.Ready)?.query?.trim().orEmpty()
                if (term.isNotEmpty()) analyticEvents.onSearchClosed(term)
                updateReady { it.copy(query = "") }
            }
            HomeIntent.SignedOut -> authRepository.signOut()
        }
    }

    private inline fun updateReady(block: (HomeUiState.Ready) -> HomeUiState.Ready) {
        val current = _state.value as? HomeUiState.Ready ?: return
        _state.value = block(current)
    }

    private fun remove(figure: Figure) {
        viewModelScope.launch {
            when (val result = repository.remove(figure.id)) {
                is FigureRemoveResult.Success -> analyticEvents.onItemDeleted(figure)
                is FigureRemoveResult.Failure ->
                    _effects.send(HomeEffect.RemoveFailed(result.cause.message ?: "Delete failed"))
            }
        }
    }
}
