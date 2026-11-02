package com.garyfimo.batmancollection.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.garyfimo.batmancollection.data.AuthRepository
import com.garyfimo.batmancollection.data.AuthState
import com.garyfimo.batmancollection.data.Figure
import com.garyfimo.batmancollection.data.FigureListResult
import com.garyfimo.batmancollection.data.FigureRepository
import com.garyfimo.batmancollection.data.FigureType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Ready(
        val items: List<Figure>,
        val filter: FigureType? = null,
    ) : HomeUiState {
        val visibleItems: List<Figure> =
            if (filter == null) items else items.filter { it.tipo == filter }
    }
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(
    private val repository: FigureRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    val authState: StateFlow<AuthState> = authRepository.state

    init {
        viewModelScope.launch {
            repository.observe().collect { result ->
                val previousFilter = (_state.value as? HomeUiState.Ready)?.filter
                _state.value = when (result) {
                    is FigureListResult.Success -> HomeUiState.Ready(result.items, previousFilter)
                    is FigureListResult.Failure ->
                        HomeUiState.Error(result.cause.message ?: "Unknown error")
                }
            }
        }
    }

    fun setFilter(filter: FigureType?) {
        val current = _state.value as? HomeUiState.Ready ?: return
        _state.value = current.copy(filter = filter)
    }

    fun signOut() {
        authRepository.signOut()
    }
}
