package com.garyfimo.batmancollection.wishlist.presentation.wishlist

import com.garyfimo.batmancollection.analytics.domain.Analytics
import com.garyfimo.batmancollection.analytics.domain.Events
import com.garyfimo.batmancollection.analytics.domain.Params
import com.garyfimo.batmancollection.wishlist.domain.WishItem

class WishlistAnalyticEvents(private val analytics: Analytics) {

    fun onScreenView() = track(Events.SCREEN_VIEW)

    fun onWishSelected(wish: WishItem) = track(
        Events.SELECT_CONTENT,
        wishParams(wish),
    )

    fun onWishDeleted(wish: WishItem) = track(
        Events.ITEM_DELETED,
        wishParams(wish),
    )

    private fun wishParams(wish: WishItem): Map<String, Any> = mapOf(
        Params.CONTENT_TYPE to CONTENT_TYPE_WISH,
        Params.ITEM_NAME to wish.nombre,
    )

    private fun track(event: String, params: Map<String, Any> = emptyMap()) {
        analytics.track(event, params + (Params.SCREEN_NAME to SCREEN_NAME))
    }

    companion object {
        const val SCREEN_NAME = "wishlist"
        private const val CONTENT_TYPE_WISH = "wish"
    }
}
