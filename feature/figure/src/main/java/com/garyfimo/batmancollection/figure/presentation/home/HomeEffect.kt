package com.garyfimo.batmancollection.figure.presentation.home

sealed interface HomeEffect {
    data class RemoveFailed(val message: String) : HomeEffect
}
