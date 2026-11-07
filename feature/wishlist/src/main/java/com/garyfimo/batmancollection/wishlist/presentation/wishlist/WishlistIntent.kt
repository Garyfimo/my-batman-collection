package com.garyfimo.batmancollection.wishlist.presentation.wishlist

import com.garyfimo.batmancollection.wishlist.domain.WishItem

sealed interface WishlistIntent {
    data class WishSelected(val wish: WishItem) : WishlistIntent
    data class WishRemoved(val wish: WishItem) : WishlistIntent
}
