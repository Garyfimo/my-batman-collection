package com.garyfimo.batmancollection.figure.presentation.home

import com.garyfimo.batmancollection.analytics.domain.Analytics
import com.garyfimo.batmancollection.analytics.domain.Events
import com.garyfimo.batmancollection.analytics.domain.Params
import com.garyfimo.batmancollection.figure.domain.Figure
import com.garyfimo.batmancollection.figure.domain.FigureType

class HomeAnalyticEvents(private val analytics: Analytics) {

    fun onScreenView() = track(Events.SCREEN_VIEW)

    fun onSortChanged(sort: HomeSort) = track(
        Events.SORT_CHANGED,
        mapOf(Params.SORT_BY to sort.name.lowercase()),
    )

    fun onFilterCleared() = track(
        Events.FILTER_CLEARED,
        mapOf(Params.FILTER_NAME to FILTER_NAME_TYPE),
    )

    fun onFilterApplied(type: FigureType) = track(
        Events.FILTER_APPLIED,
        mapOf(
            Params.FILTER_NAME to FILTER_NAME_TYPE,
            Params.FILTER_VALUE to type.name.lowercase(),
        ),
    )

    fun onFigureSelected(figure: Figure) = track(
        Events.SELECT_CONTENT,
        figureParams(figure),
    )

    fun onItemDeleted(figure: Figure) = track(
        Events.ITEM_DELETED,
        figureParams(figure),
    )

    fun onSearchOpened() = track(Events.SEARCH_OPENED)

    fun onSearchClosed(term: String) = track(
        Events.SEARCH,
        mapOf(Params.SEARCH_TERM to term.take(SEARCH_TERM_MAX_LENGTH)),
    )

    private fun figureParams(figure: Figure): Map<String, Any> = mapOf(
        Params.CONTENT_TYPE to CONTENT_TYPE_FIGURE,
        Params.ITEM_NAME to figure.nombre,
    )

    private fun track(event: String, params: Map<String, Any> = emptyMap()) {
        analytics.track(event, params + (Params.SCREEN_NAME to SCREEN_NAME))
    }

    companion object {
        const val SCREEN_NAME = "home"
        private const val FILTER_NAME_TYPE = "type"
        private const val CONTENT_TYPE_FIGURE = "figure"
        private const val SEARCH_TERM_MAX_LENGTH = 100
    }
}
