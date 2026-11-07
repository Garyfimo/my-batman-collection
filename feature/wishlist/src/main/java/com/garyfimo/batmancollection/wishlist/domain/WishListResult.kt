package com.garyfimo.batmancollection.wishlist.domain

sealed interface WishListResult {
    data class Success(val items: List<WishItem>) : WishListResult
    data class Failure(val cause: Throwable) : WishListResult
}
