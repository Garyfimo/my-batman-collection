package com.garyfimo.batmancollection.di

import com.garyfimo.batmancollection.analytics.BatmanCollectionAnalyticParameters
import com.garyfimo.batmancollection.analytics.data.CommonParamsAnalytics
import com.garyfimo.batmancollection.analytics.data.FirebaseAnalyticsImpl
import com.garyfimo.batmancollection.analytics.domain.Analytics
import com.google.firebase.analytics.FirebaseAnalytics
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single { FirebaseAnalytics.getInstance(androidContext()) }
    single<Analytics> {
        CommonParamsAnalytics(
            delegate = FirebaseAnalyticsImpl(get()),
            commonParams = BatmanCollectionAnalyticParameters.commonParams(),
        )
    }
}
