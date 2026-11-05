package com.garyfimo.batmancollection.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.garyfimo.batmancollection.data.FigureLookup
import com.garyfimo.batmancollection.data.FigureRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetailsViewModel(
    private val id: String,
    private val repository: FigureRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading)
    val state: StateFlow<DetailsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = when (val result = repository.get(id)) {
                is FigureLookup.Found -> DetailsUiState.Ready(result.figure)
                FigureLookup.NotFound -> DetailsUiState.NotFound
                is FigureLookup.Failure ->
                    DetailsUiState.Error(result.cause.message ?: "Unknown error")
            }
        }
    }
}
