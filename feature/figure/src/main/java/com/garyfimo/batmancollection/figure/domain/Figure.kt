package com.garyfimo.batmancollection.figure.domain

import com.garyfimo.batmancollection.core.domain.FigureType

data class Figure(
    val id: String,
    val tipo: FigureType,
    val nombre: String,
    val serie: String,
    val imageUrl: String,
)
