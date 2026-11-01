package com.garyfimo.batmancollection.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.compositionLocalOf

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

fun batmanImageSharedKey(id: String): String = "batman-image-$id"

fun batmanPopupImageKey(id: String): String = "batman-popup-$id"
