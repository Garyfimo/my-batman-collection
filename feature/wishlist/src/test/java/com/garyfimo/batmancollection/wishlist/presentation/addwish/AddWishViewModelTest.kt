package com.garyfimo.batmancollection.wishlist.presentation.addwish

import app.cash.turbine.test
import com.garyfimo.batmancollection.analytics.domain.Analytics
import com.garyfimo.batmancollection.analytics.domain.Events
import com.garyfimo.batmancollection.core.domain.FigureType
import com.garyfimo.batmancollection.wishlist.domain.WishAddResult
import com.garyfimo.batmancollection.wishlist.domain.WishDraft
import com.garyfimo.batmancollection.wishlist.domain.WishRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddWishViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository: WishRepository = mockk(relaxed = true)
    private val analytics = FakeAnalytics()

    private class FakeAnalytics : Analytics {
        val events = mutableListOf<Pair<String, Map<String, Any>>>()
        override fun track(event: String, params: Map<String, Any>) {
            events += event to params
        }
    }

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `fires screen_view on init`() = runTest(dispatcher) {
        AddWishViewModel(repository, analytics)
        assertTrue(analytics.events.any { it.first == Events.SCREEN_VIEW })
    }

    @Test
    fun `submit on empty form flags every required field without emitting Saved`() = runTest(dispatcher) {
        val viewModel = AddWishViewModel(repository, analytics)

        viewModel.effects.test {
            viewModel.onIntent(AddWishIntent.Submitted)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }

        val state = viewModel.state.value
        assertTrue(state.submitAttempted)
        assertNotNull(state.tipoError)
        assertNotNull(state.nombreError)
        assertNotNull(state.serieError)
        assertNotNull(state.urlError)
        assertFalse(state.submitting)
    }

    @Test
    fun `invalid url flags urlError even when other fields are filled`() = runTest(dispatcher) {
        val viewModel = AddWishViewModel(repository, analytics)
        viewModel.onIntent(AddWishIntent.TipoChanged(FigureType.Funko))
        viewModel.onIntent(AddWishIntent.NombreChanged("Batman"))
        viewModel.onIntent(AddWishIntent.SerieChanged("1187"))
        viewModel.onIntent(AddWishIntent.UrlChanged("not a url"))

        viewModel.effects.test {
            viewModel.onIntent(AddWishIntent.Submitted)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(AddWishFormState.ERROR_INVALID_URL, viewModel.state.value.urlError)
    }

    @Test
    fun `fixing a flagged field clears its error on next recomposition`() = runTest(dispatcher) {
        val viewModel = AddWishViewModel(repository, analytics)
        viewModel.onIntent(AddWishIntent.Submitted)

        viewModel.onIntent(AddWishIntent.NombreChanged("Bruce Wayne"))
        assertNull(viewModel.state.value.nombreError)
        assertNotNull(viewModel.state.value.serieError)
    }

    @Test
    fun `successful add emits Saved, resets state, and tracks wish_added`() = runTest(dispatcher) {
        coEvery { repository.add(any()) } returns WishAddResult.Success("new-id")
        val viewModel = AddWishViewModel(repository, analytics)
        viewModel.onIntent(AddWishIntent.TipoChanged(FigureType.Funko))
        viewModel.onIntent(AddWishIntent.NombreChanged("Bruce Wayne"))
        viewModel.onIntent(AddWishIntent.SerieChanged("DC Pop! Heroes"))
        viewModel.onIntent(AddWishIntent.UrlChanged("https://example.com/bruce"))

        viewModel.effects.test {
            viewModel.onIntent(AddWishIntent.Submitted)
            assertEquals(AddWishEffect.Saved, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        coVerify {
            repository.add(
                WishDraft(
                    tipo = FigureType.Funko,
                    nombre = "Bruce Wayne",
                    serie = "DC Pop! Heroes",
                    url = "https://example.com/bruce",
                ),
            )
        }
        assertEquals(AddWishFormState(), viewModel.state.value)
        assertTrue(analytics.events.any { it.first == Events.ADD_WISH_SUBMITTED })
        assertTrue(analytics.events.any { it.first == Events.WISH_ADDED })
    }

    @Test
    fun `failed add emits SubmitFailed, keeps form content, and tracks wish_add_failed`() = runTest(dispatcher) {
        coEvery { repository.add(any()) } returns
            WishAddResult.Failure(IllegalStateException("boom"))
        val viewModel = AddWishViewModel(repository, analytics)
        viewModel.onIntent(AddWishIntent.TipoChanged(FigureType.HotWheels))
        viewModel.onIntent(AddWishIntent.NombreChanged("Batmobile"))
        viewModel.onIntent(AddWishIntent.SerieChanged("103/250"))
        viewModel.onIntent(AddWishIntent.UrlChanged("https://example.com/car"))

        viewModel.effects.test {
            viewModel.onIntent(AddWishIntent.Submitted)
            advanceUntilIdle()
            assertEquals(AddWishEffect.SubmitFailed("boom"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        val state = viewModel.state.value
        assertEquals(FigureType.HotWheels, state.tipo)
        assertEquals("Batmobile", state.nombre)
        assertEquals("103/250", state.serie)
        assertEquals("https://example.com/car", state.url)
        assertFalse(state.submitting)
        assertTrue(analytics.events.any { it.first == Events.WISH_ADD_FAILED })
    }
}
