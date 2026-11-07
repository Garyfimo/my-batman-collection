package com.garyfimo.batmancollection.figure.presentation.addfigure

import com.garyfimo.batmancollection.analytics.domain.Analytics
import com.garyfimo.batmancollection.analytics.domain.Events
import com.garyfimo.batmancollection.analytics.domain.Params
import com.garyfimo.batmancollection.figure.domain.FigureDraft
import com.garyfimo.batmancollection.core.domain.FigureType

class AddFigureAnalyticEvents(private val analytics: Analytics) {

    fun onScreenView() = track(Events.SCREEN_VIEW)

    fun onSubmitted() = track(Events.ADD_FIGURE_SUBMITTED)

    fun onItemAdded(draft: FigureDraft) = track(
        Events.ITEM_ADDED,
        draftParams(draft.tipo, draft.nombre, draft.serie),
    )

    fun onItemAddFailed(tipo: FigureType?, nombre: String, serie: String, error: String) = track(
        Events.ITEM_ADD_FAILED,
        buildMap {
            putAll(draftParams(tipo, nombre, serie))
            put(Params.ERROR_MESSAGE, error.take(ERROR_MAX_LENGTH))
        },
    )

    private fun draftParams(tipo: FigureType?, nombre: String, serie: String): Map<String, Any> =
        buildMap {
            if (tipo != null) put(Params.FIGURE_TYPE, tipo.name.lowercase())
            put(Params.ITEM_NAME, nombre)
            put(Params.FIGURE_SERIES, serie)
        }

    private fun track(event: String, params: Map<String, Any> = emptyMap()) {
        analytics.track(event, params + (Params.SCREEN_NAME to SCREEN_NAME))
    }

    companion object {
        const val SCREEN_NAME = "add_figure"
        private const val ERROR_MAX_LENGTH = 100
    }
}
