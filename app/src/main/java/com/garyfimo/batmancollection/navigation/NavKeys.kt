package com.garyfimo.batmancollection.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object HomeKey : NavKey

@Serializable
data class DetailsKey(val id: String) : NavKey
