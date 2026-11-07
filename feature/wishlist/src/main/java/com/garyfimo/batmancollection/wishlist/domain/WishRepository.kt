package com.garyfimo.batmancollection.wishlist.domain

import kotlinx.coroutines.flow.Flow

interface WishRepository {
    fun observe(): Flow<WishListResult>
    suspend fun add(draft: WishDraft): WishAddResult
    suspend fun remove(id: String): WishRemoveResult
}
