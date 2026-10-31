package com.garyfimo.batmancollection

import android.app.Application
import com.garyfimo.batmancollection.di.appModule
import com.garyfimo.batmancollection.di.networkModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class BatmanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.INFO)
            androidContext(this@BatmanApp)
            modules(networkModule, appModule)
        }
    }
}
