package com.garyfimo.batmancollection.wishlist.domain

sealed interface WishAddResult {
    data class Success(val id: String) : WishAddResult
    data class Failure(val cause: Throwable) : WishAddResult
}
