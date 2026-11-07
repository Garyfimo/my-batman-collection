package com.garyfimo.batmancollection.wishlist.domain

sealed interface WishRemoveResult {
    data object Success : WishRemoveResult
    data class Failure(val cause: Throwable) : WishRemoveResult
}
