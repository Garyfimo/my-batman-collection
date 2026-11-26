package com.garyfimo.batmancollection.figure.presentation.addfigure

import app.cash.turbine.test
import com.garyfimo.batmancollection.analytics.domain.Analytics
import com.garyfimo.batmancollection.figure.domain.Figure
import com.garyfimo.batmancollection.figure.domain.FigureAddResult
import com.garyfimo.batmancollection.figure.domain.FigureDraft
import com.garyfimo.batmancollection.figure.domain.FigureLookup
import com.garyfimo.batmancollection.figure.domain.FigureRepository
import com.garyfimo.batmancollection.figure.domain.FigureUpdateResult
import com.garyfimo.batmancollection.core.domain.FigureType
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
class AddFigureViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository: FigureRepository = mockk(relaxed = true)
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
    fun `submit on empty form flags every required field without emitting Submitted`() = runTest(dispatcher) {
        val viewModel = AddFigureViewModel(repository, analytics)

        viewModel.effects.test {
            viewModel.onIntent(AddFigureIntent.Submitted)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }

        val state = viewModel.state.value
        assertTrue(state.submitAttempted)
        assertNotNull(state.tipoError)
        assertNotNull(state.nombreError)
        assertNotNull(state.serieError)
        assertNotNull(state.imageError)
        coVerify(exactly = 0) { repository.add(any()) }
    }

    @Test
    fun `fixing a flagged field clears its error on next recomposition`() = runTest(dispatcher) {
        val viewModel = AddFigureViewModel(repository, analytics)
        viewModel.onIntent(AddFigureIntent.Submitted)

        viewModel.onIntent(AddFigureIntent.NombreChanged("Bruce Wayne"))
        assertNull(viewModel.state.value.nombreError)
        assertNotNull(viewModel.state.value.serieError)
    }

    @Test
    fun `successful add emits Submitted and resets state`() = runTest(dispatcher) {
        coEvery { repository.add(any()) } returns FigureAddResult.Success("new-id")
        val viewModel = AddFigureViewModel(repository, analytics)
        viewModel.onIntent(AddFigureIntent.TipoChanged(FigureType.Funko))
        viewModel.onIntent(AddFigureIntent.NombreChanged("Bruce Wayne"))
        viewModel.onIntent(AddFigureIntent.SerieChanged("DC Pop! Heroes"))
        viewModel.onIntent(AddFigureIntent.ImagePicked("content://images/1"))

        viewModel.effects.test {
            viewModel.onIntent(AddFigureIntent.Submitted)
            assertEquals(AddFigureEffect.Saved, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        coVerify {
            repository.add(
                FigureDraft(
                    tipo = FigureType.Funko,
                    nombre = "Bruce Wayne",
                    serie = "DC Pop! Heroes",
                    imageUri = "content://images/1",
                ),
            )
        }
        assertEquals(AddFigureFormState(), viewModel.state.value)
    }

    @Test
    fun `editor prefills form from the existing figure`() = runTest(dispatcher) {
        val existing = Figure(
            id = "fig-1",
            tipo = FigureType.Funko,
            nombre = "Bruce Wayne",
            serie = "DC Pop! Heroes",
            imageUrl = "https://example.test/img.jpg",
        )
        coEvery { repository.get("fig-1") } returns FigureLookup.Found(existing)

        val viewModel = AddFigureViewModel(repository, analytics, existingId = "fig-1")
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("fig-1", state.existingId)
        assertTrue(state.isEditing)
        assertFalse(state.loading)
        assertEquals(FigureType.Funko, state.tipo)
        assertEquals("Bruce Wayne", state.nombre)
        assertEquals("DC Pop! Heroes", state.serie)
        assertEquals("https://example.test/img.jpg", state.imageUri)
    }

    @Test
    fun `successful edit calls update and emits Saved without resetting the form`() = runTest(dispatcher) {
        val existing = Figure(
            id = "fig-1",
            tipo = FigureType.Funko,
            nombre = "Bruce Wayne",
            serie = "DC Pop! Heroes",
            imageUrl = "https://example.test/img.jpg",
        )
        coEvery { repository.get("fig-1") } returns FigureLookup.Found(existing)
        coEvery { repository.update(any(), any()) } returns FigureUpdateResult.Success

        val viewModel = AddFigureViewModel(repository, analytics, existingId = "fig-1")
        advanceUntilIdle()
        viewModel.onIntent(AddFigureIntent.NombreChanged("Batman"))

        viewModel.effects.test {
            viewModel.onIntent(AddFigureIntent.Submitted)
            advanceUntilIdle()
            assertEquals(AddFigureEffect.Saved, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        coVerify {
            repository.update(
                "fig-1",
                FigureDraft(
                    tipo = FigureType.Funko,
                    nombre = "Batman",
                    serie = "DC Pop! Heroes",
                    imageUri = "https://example.test/img.jpg",
                ),
            )
        }
        coVerify(exactly = 0) { repository.add(any()) }
        // The form stays prefilled; the screen navigates away via the Saved effect.
        assertEquals("Batman", viewModel.state.value.nombre)
    }

    @Test
    fun `edit with NotFound figure emits SubmitFailed`() = runTest(dispatcher) {
        coEvery { repository.get("missing") } returns FigureLookup.NotFound
        val viewModel = AddFigureViewModel(repository, analytics, existingId = "missing")

        viewModel.effects.test {
            advanceUntilIdle()
            assertEquals(AddFigureEffect.SubmitFailed("Figure not found"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(viewModel.state.value.loading)
    }

    @Test
    fun `failed update emits SubmitFailed and keeps the form`() = runTest(dispatcher) {
        val existing = Figure(
            id = "fig-1",
            tipo = FigureType.HotWheels,
            nombre = "Batmobile",
            serie = "103/250",
            imageUrl = "https://example.test/img.jpg",
        )
        coEvery { repository.get("fig-1") } returns FigureLookup.Found(existing)
        coEvery { repository.update(any(), any()) } returns
            FigureUpdateResult.Failure(IllegalStateException("network"))

        val viewModel = AddFigureViewModel(repository, analytics, existingId = "fig-1")
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onIntent(AddFigureIntent.Submitted)
            advanceUntilIdle()
            assertEquals(AddFigureEffect.SubmitFailed("network"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        val state = viewModel.state.value
        assertEquals("Batmobile", state.nombre)
        assertFalse(state.submitting)
    }

    @Test
    fun `failed add emits SubmitFailed and keeps the form content`() = runTest(dispatcher) {
        coEvery { repository.add(any()) } returns
            FigureAddResult.Failure(IllegalStateException("boom"))
        val viewModel = AddFigureViewModel(repository, analytics)
        viewModel.onIntent(AddFigureIntent.TipoChanged(FigureType.HotWheels))
        viewModel.onIntent(AddFigureIntent.NombreChanged("Batmobile"))
        viewModel.onIntent(AddFigureIntent.SerieChanged("103/250"))
        viewModel.onIntent(AddFigureIntent.ImagePicked("content://images/2"))

        viewModel.effects.test {
            viewModel.onIntent(AddFigureIntent.Submitted)
            advanceUntilIdle()
            assertEquals(AddFigureEffect.SubmitFailed("boom"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        val state = viewModel.state.value
        assertEquals(FigureType.HotWheels, state.tipo)
        assertEquals("Batmobile", state.nombre)
        assertEquals("103/250", state.serie)
        assertEquals("content://images/2", state.imageUri)
        assertFalse(state.submitting)
    }
}
