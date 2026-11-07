package com.garyfimo.batmancollection.wishlist.presentation.wishlist

import com.garyfimo.batmancollection.wishlist.domain.WishItem

sealed interface WishlistUiState {
    data object Loading : WishlistUiState
    data class Ready(val items: List<WishItem>) : WishlistUiState
    data class Error(val message: String) : WishlistUiState
}
