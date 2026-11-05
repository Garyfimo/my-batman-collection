package com.garyfimo.batmancollection.figure.presentation.addfigure

sealed interface AddFigureEffect {
    data object Saved : AddFigureEffect
    data class SubmitFailed(val message: String) : AddFigureEffect
}
