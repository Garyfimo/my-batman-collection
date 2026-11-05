package com.garyfimo.batmancollection.figure.domain

sealed interface FigureListResult {
    data class Success(val items: List<Figure>) : FigureListResult
    data class Failure(val cause: Throwable) : FigureListResult
}
