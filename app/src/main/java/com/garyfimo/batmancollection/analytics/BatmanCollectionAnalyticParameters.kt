package com.garyfimo.batmancollection.analytics

import com.garyfimo.batmancollection.analytics.domain.Params

object BatmanCollectionAnalyticParameters {
    const val ID_MULTIPRODUCT_JOURNEY = "batman-collection"
    const val JOURNEY_ID = "collection"
    const val ID_EVENT = "colle"

    fun commonParams(): Map<String, Any> = mapOf(
        Params.ID_MULTIPRODUCT_JOURNEY to ID_MULTIPRODUCT_JOURNEY,
        Params.JOURNEY_ID to JOURNEY_ID,
        Params.ID_EVENT to ID_EVENT,
    )
}
