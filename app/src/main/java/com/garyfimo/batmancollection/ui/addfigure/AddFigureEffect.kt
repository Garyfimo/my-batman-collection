package com.garyfimo.batmancollection.ui.addfigure

sealed interface AddFigureEffect {
    data object Saved : AddFigureEffect
    data class SubmitFailed(val message: String) : AddFigureEffect
}
