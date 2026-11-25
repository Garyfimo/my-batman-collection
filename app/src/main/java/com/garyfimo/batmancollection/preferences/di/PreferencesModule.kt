package com.garyfimo.batmancollection.preferences.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.garyfimo.batmancollection.preferences.data.DataStorePalettePreferences
import com.garyfimo.batmancollection.preferences.domain.PalettePreferences
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

private const val THEME_PREFS_NAME = "theme-prefs"

val preferencesModule = module {
    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.create(
            produceFile = { androidContext().preferencesDataStoreFile(THEME_PREFS_NAME) },
        )
    }
    single<PalettePreferences> { DataStorePalettePreferences(get()) }
}
