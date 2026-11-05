package com.garyfimo.batmancollection.analytics.data

import android.os.Bundle
import com.garyfimo.batmancollection.analytics.domain.Analytics
import com.google.firebase.analytics.FirebaseAnalytics

class FirebaseAnalyticsImpl(
    private val firebase: FirebaseAnalytics,
) : Analytics {
    override fun track(event: String, params: Map<String, Any>) {
        firebase.logEvent(event, params.toAnalyticsBundle())
    }
}

private fun Map<String, Any>.toAnalyticsBundle(): Bundle {
    val bundle = Bundle(size)
    for ((key, value) in this) {
        when (value) {
            is String -> bundle.putString(key, value)
            is Int -> bundle.putInt(key, value)
            is Long -> bundle.putLong(key, value)
            is Double -> bundle.putDouble(key, value)
            is Float -> bundle.putFloat(key, value)
            is Boolean -> bundle.putBoolean(key, value)
            else -> bundle.putString(key, value.toString())
        }
    }
    return bundle
}
