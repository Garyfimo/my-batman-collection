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

        viewModel.events.test {
            viewModel.submit()
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
        viewModel.submit()

        viewModel.setNombre("Bruce Wayne")
        assertNull(viewModel.state.value.nombreError)
        assertNotNull(viewModel.state.value.serieError)
    }

    @Test
    fun `successful add emits Submitted and resets state`() = runTest(dispatcher) {
        coEvery { repository.add(any()) } returns FigureAddResult.Success("new-id")
        val viewModel = AddFigureViewModel(repository)
        viewModel.setTipo(FigureType.Funko)
        viewModel.setNombre("Bruce Wayne")
        viewModel.setSerie("DC Pop! Heroes")
        viewModel.setImageUri("content://images/1")

        viewModel.events.test {
            viewModel.submit()
            assertEquals(AddFigureEvent.Submitted, awaitItem())
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
        viewModel.setTipo(FigureType.HotWheels)
        viewModel.setNombre("Batmobile")
        viewModel.setSerie("103/250")
        viewModel.setImageUri("content://images/2")

        viewModel.events.test {
            viewModel.submit()
            advanceUntilIdle()
            assertEquals(AddFigureEvent.SubmitFailed("boom"), awaitItem())
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
