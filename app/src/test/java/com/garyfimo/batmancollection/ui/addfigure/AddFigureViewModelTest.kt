package com.garyfimo.batmancollection.ui.addfigure

import app.cash.turbine.test
import com.garyfimo.batmancollection.data.FigureType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddFigureViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `submit on empty form flags every required field without emitting Submitted`() = runTest(dispatcher) {
        val viewModel = AddFigureViewModel()

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
    }

    @Test
    fun `fixing a flagged field clears its error on next recomposition`() = runTest(dispatcher) {
        val viewModel = AddFigureViewModel()
        viewModel.submit()

        viewModel.setNombre("Bruce Wayne")
        assertNull(viewModel.state.value.nombreError)
        assertNotNull(viewModel.state.value.serieError)
    }

    @Test
    fun `fully valid form emits Submitted and resets state`() = runTest(dispatcher) {
        val viewModel = AddFigureViewModel()
        viewModel.setTipo(FigureType.Funko)
        viewModel.setNombre("Bruce Wayne")
        viewModel.setSerie("DC Pop! Heroes")
        viewModel.setImageUri("content://images/1")

        viewModel.events.test {
            viewModel.submit()
            assertEquals(AddFigureEvent.Submitted, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        val state = viewModel.state.value
        assertEquals(AddFigureFormState(), state)
    }

    @Test
    fun `clear wipes every field`() = runTest(dispatcher) {
        val viewModel = AddFigureViewModel()
        viewModel.setTipo(FigureType.HotWheels)
        viewModel.setNombre("Batmobile")
        viewModel.setSerie("Elite 64")
        viewModel.setImageUri("content://images/2")

        viewModel.clear()

        assertEquals(AddFigureFormState(), viewModel.state.value)
    }
}
