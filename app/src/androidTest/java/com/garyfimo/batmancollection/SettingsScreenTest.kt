package com.garyfimo.batmancollection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.garyfimo.batmancollection.analytics.domain.Analytics
import com.garyfimo.batmancollection.core.domain.FigureType
import com.garyfimo.batmancollection.figure.domain.Figure
import com.garyfimo.batmancollection.figure.domain.FigureListResult
import com.garyfimo.batmancollection.figure.domain.FigureRepository
import com.garyfimo.batmancollection.figure.presentation.home.HomeViewModel
import com.garyfimo.batmancollection.figure.presentation.home.TAG_HOME_FILTER_PREFIX
import com.garyfimo.batmancollection.figure.presentation.home.TAG_HOME_ROOT
import com.garyfimo.batmancollection.figure.presentation.home.TAG_HOME_SETTINGS
import com.garyfimo.batmancollection.login.domain.AuthRepository
import com.garyfimo.batmancollection.login.domain.AuthState
import com.garyfimo.batmancollection.navigation.BatmanNavDisplay
import com.garyfimo.batmancollection.preferences.domain.PalettePreferences
import com.garyfimo.batmancollection.settings.presentation.SettingsAnalyticEvents
import com.garyfimo.batmancollection.settings.presentation.SettingsScreen
import com.garyfimo.batmancollection.settings.presentation.SettingsViewModel
import com.garyfimo.batmancollection.settings.presentation.TAG_SETTINGS_BACK
import com.garyfimo.batmancollection.settings.presentation.TAG_SETTINGS_RADIO_CLASSIC
import com.garyfimo.batmancollection.settings.presentation.TAG_SETTINGS_RADIO_NOIR
import com.garyfimo.batmancollection.settings.presentation.TAG_SETTINGS_ROOT
import com.garyfimo.batmancollection.settings.presentation.TAG_SETTINGS_ROW_CLASSIC
import com.garyfimo.batmancollection.settings.presentation.TAG_SETTINGS_ROW_NOIR
import com.garyfimo.batmancollection.ui.theme.BatmanCollectionTheme
import com.garyfimo.batmancollection.ui.theme.BatmanPalette
import com.garyfimo.batmancollection.wishlist.domain.WishRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val figure = Figure(
        id = "funko-the-batman-1187",
        tipo = FigureType.Funko,
        nombre = "The Batman",
        serie = "1187",
        imageUrl = "https://example.com/1187.jpg",
    )

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun preferences(initial: BatmanPalette) = mockk<PalettePreferences>(relaxed = true).also {
        every { it.palette() } returns flowOf(initial)
    }

    private fun startKoinWith(prefs: PalettePreferences) {
        runCatching { stopKoin() }
        val repository = mockk<FigureRepository>(relaxed = true).also {
            every { it.observe() } returns flowOf(FigureListResult.Success(listOf(figure)))
        }
        startKoin {
            modules(
                module {
                    single<PalettePreferences> { prefs }
                    single<FigureRepository> { repository }
                    single<WishRepository> { mockk(relaxed = true) }
                    single<AuthRepository> {
                        mockk<AuthRepository>().also {
                            every { it.state } returns MutableStateFlow<AuthState>(AuthState.SignedOut)
                        }
                    }
                    single<Analytics> {
                        object : Analytics {
                            override fun track(event: String, params: Map<String, Any>) = Unit
                        }
                    }
                    factory { SettingsAnalyticEvents(get()) }
                    viewModelOf(::SettingsViewModel)
                    viewModelOf(::HomeViewModel)
                },
            )
        }
    }

    @Test
    fun shows_both_palette_options() {
        startKoinWith(preferences(BatmanPalette.CLASSIC))
        composeRule.setContent { BatmanCollectionTheme { SettingsScreen(onBack = {}) } }

        composeRule.onNodeWithTag(TAG_SETTINGS_ROW_CLASSIC).assertIsDisplayed()
        composeRule.onNodeWithTag(TAG_SETTINGS_ROW_NOIR).assertIsDisplayed()
        composeRule.onNodeWithTag(TAG_SETTINGS_RADIO_CLASSIC, useUnmergedTree = true).assertIsSelected()
        composeRule.onNodeWithTag(TAG_SETTINGS_RADIO_NOIR, useUnmergedTree = true).assertIsNotSelected()
    }

    @Test
    fun tapping_noir_row_selects_it() {
        val prefs = preferences(BatmanPalette.CLASSIC)
        startKoinWith(prefs)
        composeRule.setContent { BatmanCollectionTheme { SettingsScreen(onBack = {}) } }

        composeRule.onNodeWithTag(TAG_SETTINGS_ROW_NOIR).performClick()
        composeRule.waitForIdle()

        coVerify { prefs.setPalette(BatmanPalette.NOIR) }
    }

    @Test
    fun navigating_to_settings_and_back_preserves_home_filter_selection() {
        startKoinWith(preferences(BatmanPalette.CLASSIC))
        composeRule.setContent { BatmanCollectionTheme { BatmanNavDisplay() } }
        composeRule.mainClock.autoAdvance = true
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText(figure.nombre).fetchSemanticsNodes().isNotEmpty()
        }

        val filterTag = "$TAG_HOME_FILTER_PREFIX${FigureType.Funko.name}"
        composeRule.onNodeWithTag(filterTag).performClick()
        composeRule.onNodeWithTag(filterTag).assertIsSelected()

        composeRule.onNodeWithTag(TAG_HOME_SETTINGS).performClick()
        composeRule.mainClock.advanceTimeBy(1_000L)
        composeRule.waitForIdle()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(TAG_SETTINGS_ROOT).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag(TAG_SETTINGS_BACK).performClick()
        composeRule.mainClock.advanceTimeBy(1_000L)
        composeRule.waitForIdle()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(TAG_HOME_ROOT).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag(filterTag).assertIsSelected()
    }

    @Test
    fun toggling_system_dark_mode_on_settings_keeps_noir_selected() {
        startKoinWith(preferences(BatmanPalette.NOIR))
        var dark by mutableStateOf(false)
        composeRule.setContent {
            BatmanCollectionTheme(palette = BatmanPalette.NOIR, darkTheme = dark) {
                SettingsScreen(onBack = {})
            }
        }
        composeRule.onNodeWithTag(TAG_SETTINGS_RADIO_NOIR, useUnmergedTree = true).assertIsSelected()

        dark = true
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TAG_SETTINGS_RADIO_NOIR, useUnmergedTree = true).assertIsSelected()
        composeRule.onNodeWithTag(TAG_SETTINGS_RADIO_CLASSIC, useUnmergedTree = true).assertIsNotSelected()
    }
}
