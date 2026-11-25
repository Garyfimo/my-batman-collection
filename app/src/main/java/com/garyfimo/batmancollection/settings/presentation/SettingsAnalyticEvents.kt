package com.garyfimo.batmancollection.settings.presentation

import com.garyfimo.batmancollection.analytics.domain.Analytics
import com.garyfimo.batmancollection.analytics.domain.Events
import com.garyfimo.batmancollection.analytics.domain.Params
import com.garyfimo.batmancollection.ui.theme.BatmanPalette

class SettingsAnalyticEvents(private val analytics: Analytics) {

    fun onPaletteChanged(from: BatmanPalette, to: BatmanPalette) = track(
        Events.PALETTE_CHANGED,
        mapOf(
            Params.PALETTE_FAMILY_FROM to from.name.lowercase(),
            Params.PALETTE_FAMILY_TO to to.name.lowercase(),
        ),
    )

    private fun track(event: String, params: Map<String, Any> = emptyMap()) {
        analytics.track(event, params + (Params.SCREEN_NAME to SCREEN_NAME))
    }

    companion object {
        const val SCREEN_NAME = "settings"
    }
}
