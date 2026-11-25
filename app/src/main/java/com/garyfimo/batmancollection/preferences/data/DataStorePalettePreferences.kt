package com.garyfimo.batmancollection.preferences.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.garyfimo.batmancollection.preferences.domain.PalettePreferences
import com.garyfimo.batmancollection.ui.theme.BatmanPalette
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class DataStorePalettePreferences(
    private val dataStore: DataStore<Preferences>,
) : PalettePreferences {

    private val key = stringPreferencesKey("palette")

    override fun palette(): Flow<BatmanPalette> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { prefs ->
            val stored = prefs[key]
            BatmanPalette.entries.firstOrNull { it.name == stored } ?: BatmanPalette.CLASSIC
        }

    override suspend fun setPalette(palette: BatmanPalette) {
        dataStore.edit { it[key] = palette.name }
    }

    override suspend fun initialPalette(): BatmanPalette = palette().first()
}
