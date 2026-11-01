package com.garyfimo.batmancollection.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.garyfimo.batmancollection.data.Batman
import com.garyfimo.batmancollection.data.BatmanListResult
import com.garyfimo.batmancollection.data.BatmanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Ready(val items: List<Batman>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(private val repository: BatmanRepository) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = HomeUiState.Loading
            _state.value = when (val result = repository.list()) {
                is BatmanListResult.Success -> HomeUiState.Ready(result.items)
                is BatmanListResult.Failure ->
                    HomeUiState.Error(result.cause.message ?: "Unknown error")
            }
        }
    }
}
