package com.garyfimo.batmancollection.wishlist.presentation.wishlist

sealed interface WishlistEffect {
    data class OpenUrl(val url: String) : WishlistEffect
    data class RemoveFailed(val message: String) : WishlistEffect
}
