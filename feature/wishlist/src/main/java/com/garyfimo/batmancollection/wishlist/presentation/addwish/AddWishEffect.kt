package com.garyfimo.batmancollection.wishlist.presentation.addwish

sealed interface AddWishEffect {
    data object Saved : AddWishEffect
    data class SubmitFailed(val message: String) : AddWishEffect
}
