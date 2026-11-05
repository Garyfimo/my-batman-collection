package com.garyfimo.batmancollection.analytics.domain

interface Analytics {
    fun track(event: String, params: Map<String, Any> = emptyMap())
}
