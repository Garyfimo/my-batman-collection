package com.garyfimo.batmancollection.settings.presentation

import com.garyfimo.batmancollection.preferences.domain.PalettePreferences
import com.garyfimo.batmancollection.ui.theme.BatmanPalette
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val preferences = mockk<PalettePreferences>()
    private val analyticEvents = mockk<SettingsAnalyticEvents>(relaxed = true)

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `state mirrors stored palette`() = runTest(dispatcher) {
        every { preferences.palette() } returns flowOf(BatmanPalette.NOIR)

        val vm = SettingsViewModel(preferences, analyticEvents)
        advanceUntilIdle()

        assertEquals(BatmanPalette.NOIR, vm.state.value.selected)
    }

    @Test
    fun `PaletteSelected writes to preferences and tracks analytics`() = runTest(dispatcher) {
        every { preferences.palette() } returns flowOf(BatmanPalette.CLASSIC)
        coEvery { preferences.setPalette(any()) } returns Unit

        val vm = SettingsViewModel(preferences, analyticEvents)
        advanceUntilIdle()
        vm.onIntent(SettingsIntent.PaletteSelected(BatmanPalette.NOIR))
        advanceUntilIdle()

        coVerify { preferences.setPalette(BatmanPalette.NOIR) }
        verify { analyticEvents.onPaletteChanged(BatmanPalette.CLASSIC, BatmanPalette.NOIR) }
    }

    @Test
    fun `selecting the already-selected family does not emit palette_changed`() = runTest(dispatcher) {
        every { preferences.palette() } returns flowOf(BatmanPalette.CLASSIC)
        coEvery { preferences.setPalette(any()) } returns Unit

        val vm = SettingsViewModel(preferences, analyticEvents)
        advanceUntilIdle()
        vm.onIntent(SettingsIntent.PaletteSelected(BatmanPalette.CLASSIC))
        advanceUntilIdle()

        coVerify(exactly = 0) { preferences.setPalette(any()) }
        verify(exactly = 0) { analyticEvents.onPaletteChanged(any(), any()) }
    }
}
