package com.garyfimo.batmancollection.figure.presentation.home

import com.garyfimo.batmancollection.figure.domain.FigureType

sealed interface HomeIntent {
    data class FilterChanged(val filter: FigureType?) : HomeIntent
    data class QueryChanged(val query: String) : HomeIntent
    data class SortChanged(val sort: HomeSort) : HomeIntent
    data class FigureRemoved(val id: String) : HomeIntent
    data object SignedOut : HomeIntent
}
