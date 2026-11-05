package com.garyfimo.batmancollection.figure.presentation.addfigure

import com.garyfimo.batmancollection.figure.domain.FigureType

sealed interface AddFigureIntent {
    data class TipoChanged(val tipo: FigureType) : AddFigureIntent
    data class NombreChanged(val value: String) : AddFigureIntent
    data class SerieChanged(val value: String) : AddFigureIntent
    data class ImagePicked(val uri: String?) : AddFigureIntent
    data object Submitted : AddFigureIntent
}
