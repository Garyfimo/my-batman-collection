package com.garyfimo.batmancollection.wishlist.presentation.addwish

import com.garyfimo.batmancollection.core.domain.FigureType

sealed interface AddWishIntent {
    data class TipoChanged(val tipo: FigureType) : AddWishIntent
    data class NombreChanged(val value: String) : AddWishIntent
    data class SerieChanged(val value: String) : AddWishIntent
    data class UrlChanged(val value: String) : AddWishIntent
    data object Submitted : AddWishIntent
}
