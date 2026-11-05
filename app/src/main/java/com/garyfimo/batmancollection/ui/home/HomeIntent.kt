package com.garyfimo.batmancollection.ui.home

import com.garyfimo.batmancollection.data.FigureType

sealed interface HomeIntent {
    data class FilterChanged(val filter: FigureType?) : HomeIntent
    data class QueryChanged(val query: String) : HomeIntent
    data class SortChanged(val sort: HomeSort) : HomeIntent
    data class FigureRemoved(val id: String) : HomeIntent
    data object SignedOut : HomeIntent
}
