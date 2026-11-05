package com.garyfimo.batmancollection.figure.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.garyfimo.batmancollection.login.domain.AuthRepository
import com.garyfimo.batmancollection.login.domain.AuthState
import com.garyfimo.batmancollection.figure.domain.FigureListResult
import com.garyfimo.batmancollection.figure.domain.FigureRemoveResult
import com.garyfimo.batmancollection.figure.domain.FigureRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: FigureRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    val authState: StateFlow<AuthState> = authRepository.state

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
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
            is HomeIntent.FilterChanged -> updateReady { it.copy(filter = intent.filter) }
            is HomeIntent.QueryChanged -> updateReady { it.copy(query = intent.query) }
            is HomeIntent.SortChanged -> updateReady { it.copy(sort = intent.sort) }
            is HomeIntent.FigureRemoved -> remove(intent.id)
            HomeIntent.SignedOut -> authRepository.signOut()
        }
    }

    private inline fun updateReady(block: (HomeUiState.Ready) -> HomeUiState.Ready) {
        val current = _state.value as? HomeUiState.Ready ?: return
        _state.value = block(current)
    }

    private fun remove(id: String) {
        viewModelScope.launch {
            when (val result = repository.remove(id)) {
                is FigureRemoveResult.Success -> Unit // snapshot flow refreshes the list
                is FigureRemoveResult.Failure ->
                    _effects.send(HomeEffect.RemoveFailed(result.cause.message ?: "Delete failed"))
            }
        }
    }
}
