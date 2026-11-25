package com.garyfimo.batmancollection

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.garyfimo.batmancollection.navigation.BatmanNavDisplay
import com.garyfimo.batmancollection.preferences.domain.PalettePreferences
import com.garyfimo.batmancollection.ui.theme.BatmanCollectionTheme
import com.garyfimo.batmancollection.ui.theme.BatmanPalette
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val preferences: PalettePreferences by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)

        var initialPalette: BatmanPalette? = null
        splash.setKeepOnScreenCondition { initialPalette == null }

        lifecycleScope.launch {
            val initial = preferences.initialPalette()
            initialPalette = initial
            setContent {
                val palette by preferences.palette()
                    .collectAsStateWithLifecycle(initialValue = initial)
                BatmanCollectionTheme(palette = palette) {
                    BatmanNavDisplay()
                }
            }
        }
    }
}
