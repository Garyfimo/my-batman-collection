package com.garyfimo.batmancollection.figure.domain

sealed interface FigureLookup {
    data class Found(val figure: Figure) : FigureLookup
    data object NotFound : FigureLookup
    data class Failure(val cause: Throwable) : FigureLookup
}
