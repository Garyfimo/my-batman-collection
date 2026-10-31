package com.garyfimo.batmancollection.data

import kotlinx.serialization.Serializable

@Serializable
data class Batman(
    val id: String,
    val name: String,
    val alterEgo: String,
    val era: String,
    val firstAppearance: String,
    val description: String,
)
