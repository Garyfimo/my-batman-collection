package com.garyfimo.batmancollection.figure.presentation.details

import app.cash.turbine.test
import com.garyfimo.batmancollection.figure.domain.Figure
import com.garyfimo.batmancollection.figure.domain.FigureLookup
import com.garyfimo.batmancollection.figure.domain.FigureRepository
import com.garyfimo.batmancollection.core.domain.FigureType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository: FigureRepository = mockk()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private val sampleFigure = Figure(
        id = "funko-the-batman-1187",
        tipo = FigureType.Funko,
        nombre = "The Batman",
        serie = "1187",
        imageUrl = "https://example.com/1187.jpg",
    )

    @Test
    fun `emits Ready when repository returns Found`() = runTest(dispatcher) {
        coEvery { repository.get(sampleFigure.id) } returns FigureLookup.Found(sampleFigure)

        val viewModel = DetailsViewModel(id = sampleFigure.id, repository = repository)

        viewModel.state.test {
            assertEquals(DetailsUiState.Loading, awaitItem())
            assertEquals(DetailsUiState.Ready(sampleFigure), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits NotFound when repository returns NotFound`() = runTest(dispatcher) {
        coEvery { repository.get("ghost") } returns FigureLookup.NotFound

        val viewModel = DetailsViewModel(id = "ghost", repository = repository)

        viewModel.state.test {
            assertEquals(DetailsUiState.Loading, awaitItem())
            assertEquals(DetailsUiState.NotFound, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Error when repository returns Failure`() = runTest(dispatcher) {
        coEvery { repository.get(sampleFigure.id) } returns
            FigureLookup.Failure(IllegalStateException("boom"))

        val viewModel = DetailsViewModel(id = sampleFigure.id, repository = repository)

        viewModel.state.test {
            assertEquals(DetailsUiState.Loading, awaitItem())
            val error = awaitItem()
            assertTrue(error is DetailsUiState.Error)
            assertEquals("boom", (error as DetailsUiState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
