package com.garyfimo.batmancollection.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.garyfimo.batmancollection.preferences.domain.PalettePreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferences: PalettePreferences,
    private val analyticEvents: SettingsAnalyticEvents,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = preferences.palette()
        .map { SettingsUiState(selected = it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.PaletteSelected -> onPaletteSelected(intent)
        }
    }

    private fun onPaletteSelected(intent: SettingsIntent.PaletteSelected) {
        val current = state.value.selected
        if (intent.palette == current) return
        viewModelScope.launch {
            preferences.setPalette(intent.palette)
            analyticEvents.onPaletteChanged(from = current, to = intent.palette)
        }
    }
}
