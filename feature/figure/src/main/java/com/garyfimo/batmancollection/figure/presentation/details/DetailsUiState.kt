package com.garyfimo.batmancollection.figure.presentation.details

import com.garyfimo.batmancollection.figure.domain.Figure

sealed interface DetailsUiState {
    data object Loading : DetailsUiState
    data class Ready(val figure: Figure) : DetailsUiState
    data object NotFound : DetailsUiState
    data class Error(val message: String) : DetailsUiState
}
