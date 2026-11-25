@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.garyfimo.batmancollection.ui.theme

import android.app.Activity
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView

@Composable
fun BatmanCollectionTheme(
    palette: BatmanPalette = BatmanPalette.CLASSIC,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = when (palette) {
        BatmanPalette.CLASSIC -> if (darkTheme) ClassicDarkColorScheme else ClassicLightColorScheme
        BatmanPalette.NOIR -> if (darkTheme) NoirDarkColorScheme else NoirLightColorScheme
        BatmanPalette.DARK_KNIGHT -> if (darkTheme) DarkKnightDarkColorScheme else DarkKnightLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity
            if (activity is ComponentActivity) {
                val style = SystemBarStyle.auto(
                    lightScrim = Color.TRANSPARENT,
                    darkScrim = Color.TRANSPARENT,
                    detectDarkMode = { darkTheme },
                )
                activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
        }
    }

    MaterialExpressiveTheme(colorScheme = colorScheme, content = content)
}
