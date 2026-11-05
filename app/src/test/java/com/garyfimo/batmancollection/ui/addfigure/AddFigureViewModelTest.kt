package com.garyfimo.batmancollection.ui.addfigure

import app.cash.turbine.test
import com.garyfimo.batmancollection.data.FigureAddResult
import com.garyfimo.batmancollection.data.FigureDraft
import com.garyfimo.batmancollection.data.FigureRepository
import com.garyfimo.batmancollection.data.FigureType
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

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `submit on empty form flags every required field without emitting Submitted`() = runTest(dispatcher) {
        val viewModel = AddFigureViewModel(repository)

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
        val viewModel = AddFigureViewModel(repository)
        viewModel.onIntent(AddFigureIntent.Submitted)

        viewModel.onIntent(AddFigureIntent.NombreChanged("Bruce Wayne"))
        assertNull(viewModel.state.value.nombreError)
        assertNotNull(viewModel.state.value.serieError)
    }

    @Test
    fun `successful add emits Submitted and resets state`() = runTest(dispatcher) {
        coEvery { repository.add(any()) } returns FigureAddResult.Success("new-id")
        val viewModel = AddFigureViewModel(repository)
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
    fun `failed add emits SubmitFailed and keeps the form content`() = runTest(dispatcher) {
        coEvery { repository.add(any()) } returns
            FigureAddResult.Failure(IllegalStateException("boom"))
        val viewModel = AddFigureViewModel(repository)
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
