package com.garyfimo.batmancollection.wishlist.domain

import com.garyfimo.batmancollection.core.domain.FigureType

data class WishDraft(
    val tipo: FigureType,
    val nombre: String,
    val serie: String,
    val url: String,
)
