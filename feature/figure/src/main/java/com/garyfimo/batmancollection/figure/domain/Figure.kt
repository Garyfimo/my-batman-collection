package com.garyfimo.batmancollection.figure.domain

data class Figure(
    val id: String,
    val tipo: FigureType,
    val nombre: String,
    val serie: String,
    val imageUrl: String,
)
