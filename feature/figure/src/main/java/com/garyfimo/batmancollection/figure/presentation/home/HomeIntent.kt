package com.garyfimo.batmancollection.figure.presentation.home

import com.garyfimo.batmancollection.figure.domain.Figure
import com.garyfimo.batmancollection.core.domain.FigureType

sealed interface HomeIntent {
    data class FilterChanged(val filter: FigureType?) : HomeIntent
    data class QueryChanged(val query: String) : HomeIntent
    data class SortChanged(val sort: HomeSort) : HomeIntent
    data class FigureSelected(val figure: Figure) : HomeIntent
    data class FigureRemoved(val figure: Figure) : HomeIntent
    data object SearchOpened : HomeIntent
    data object SearchClosed : HomeIntent
    data object SignedOut : HomeIntent
}
