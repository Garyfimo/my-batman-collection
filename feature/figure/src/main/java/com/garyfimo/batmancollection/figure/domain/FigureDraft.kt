package com.garyfimo.batmancollection.figure.domain

import com.garyfimo.batmancollection.core.domain.FigureType

data class FigureDraft(
    val tipo: FigureType,
    val nombre: String,
    val serie: String,
    val imageUri: String,
)
