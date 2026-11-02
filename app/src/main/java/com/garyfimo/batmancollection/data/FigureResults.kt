package com.garyfimo.batmancollection.data

sealed interface FigureListResult {
    data class Success(val items: List<Figure>) : FigureListResult
    data class Failure(val cause: Throwable) : FigureListResult
}

sealed interface FigureLookup {
    data class Found(val figure: Figure) : FigureLookup
    data object NotFound : FigureLookup
    data class Failure(val cause: Throwable) : FigureLookup
}

sealed interface FigureAddResult {
    data class Success(val id: String) : FigureAddResult
    data class Failure(val cause: Throwable) : FigureAddResult
}

sealed interface FigureRemoveResult {
    data object Success : FigureRemoveResult
    data class Failure(val cause: Throwable) : FigureRemoveResult
}
