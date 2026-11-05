package com.garyfimo.batmancollection.figure.domain

sealed interface FigureAddResult {
    data class Success(val id: String) : FigureAddResult
    data class Failure(val cause: Throwable) : FigureAddResult
}
