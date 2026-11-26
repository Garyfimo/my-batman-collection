package com.garyfimo.batmancollection.figure.domain

sealed interface FigureUpdateResult {
    data object Success : FigureUpdateResult
    data object NotFound : FigureUpdateResult
    data class Failure(val cause: Throwable) : FigureUpdateResult
}
