package com.garyfimo.batmancollection.preferences.domain

import com.garyfimo.batmancollection.ui.theme.BatmanPalette
import kotlinx.coroutines.flow.Flow

interface PalettePreferences {
    /** Emits the selected palette, defaulting to CLASSIC when nothing valid is stored. */
    fun palette(): Flow<BatmanPalette>

    /** Persists the user's selected palette. */
    suspend fun setPalette(palette: BatmanPalette)

    /** Returns the currently stored palette once (CLASSIC if none or unknown). */
    suspend fun initialPalette(): BatmanPalette
}
