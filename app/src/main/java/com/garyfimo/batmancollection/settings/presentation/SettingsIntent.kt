package com.garyfimo.batmancollection.settings.presentation

import com.garyfimo.batmancollection.ui.theme.BatmanPalette

sealed interface SettingsIntent {
    data class PaletteSelected(val palette: BatmanPalette) : SettingsIntent
}
