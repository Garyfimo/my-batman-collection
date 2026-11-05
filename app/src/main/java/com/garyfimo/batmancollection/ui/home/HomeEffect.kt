package com.garyfimo.batmancollection.ui.home

sealed interface HomeEffect {
    data class RemoveFailed(val message: String) : HomeEffect
}
