package com.garyfimo.batmancollection.figure.domain

sealed interface FigureRemoveResult {
    data object Success : FigureRemoveResult
    data class Failure(val cause: Throwable) : FigureRemoveResult
}
