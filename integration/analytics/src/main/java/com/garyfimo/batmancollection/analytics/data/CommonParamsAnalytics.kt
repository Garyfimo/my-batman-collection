package com.garyfimo.batmancollection.analytics.data

import com.garyfimo.batmancollection.analytics.domain.Analytics

class CommonParamsAnalytics(
    private val delegate: Analytics,
    private val commonParams: Map<String, Any>,
) : Analytics {
    override fun track(event: String, params: Map<String, Any>) {
        delegate.track(event, params + commonParams)
    }
}
