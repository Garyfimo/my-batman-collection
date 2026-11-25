package com.garyfimo.batmancollection.preferences.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.garyfimo.batmancollection.ui.theme.BatmanPalette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStorePalettePreferencesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var preferences: DataStorePalettePreferences

    @Before
    fun setUp() {
        dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { tempFolder.newFile("theme-prefs.preferences_pb") },
        )
        preferences = DataStorePalettePreferences(dataStore)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `palette emits CLASSIC on fresh install`() = runBlocking {
        assertEquals(BatmanPalette.CLASSIC, preferences.palette().first())
    }

    @Test
    fun `setPalette NOIR then palette emits NOIR`() = runBlocking {
        preferences.setPalette(BatmanPalette.NOIR)
        assertEquals(BatmanPalette.NOIR, preferences.palette().first())
    }

    @Test
    fun `initialPalette returns stored NOIR after setPalette NOIR`() = runBlocking {
        preferences.setPalette(BatmanPalette.NOIR)
        assertEquals(BatmanPalette.NOIR, preferences.initialPalette())
    }

    @Test
    fun `palette emits CLASSIC when stored value is unknown`() = runBlocking {
        dataStore.edit { it[stringPreferencesKey("palette")] = "WONDERWOMAN" }
        assertEquals(BatmanPalette.CLASSIC, preferences.palette().first())
        assertEquals(BatmanPalette.CLASSIC, preferences.initialPalette())
    }
}
