package com.garyfimo.batmancollection.ui.details

import com.garyfimo.batmancollection.data.Figure

sealed interface DetailsUiState {
    data object Loading : DetailsUiState
    data class Ready(val figure: Figure) : DetailsUiState
    data object NotFound : DetailsUiState
    data class Error(val message: String) : DetailsUiState
}
