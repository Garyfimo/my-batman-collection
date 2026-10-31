package com.garyfimo.batmancollection.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.garyfimo.batmancollection.data.Batman
import com.garyfimo.batmancollection.data.BatmanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DetailsUiState {
    data object Loading : DetailsUiState
    data class Ready(val batman: Batman) : DetailsUiState
    data object NotFound : DetailsUiState
    data class Error(val message: String) : DetailsUiState
}

class DetailsViewModel(
    private val id: String,
    private val repository: BatmanRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading)
    val state: StateFlow<DetailsUiState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = DetailsUiState.Loading
            _state.value = runCatching { repository.get(id) }
                .fold(
                    onSuccess = { batman ->
                        if (batman == null) DetailsUiState.NotFound
                        else DetailsUiState.Ready(batman)
                    },
                    onFailure = { DetailsUiState.Error(it.message ?: "Unknown error") },
                )
        }
    }
}
