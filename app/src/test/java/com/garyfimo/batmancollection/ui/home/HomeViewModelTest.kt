package com.garyfimo.batmancollection.ui.home

import app.cash.turbine.test
import com.garyfimo.batmancollection.data.Figure
import com.garyfimo.batmancollection.data.FigureListResult
import com.garyfimo.batmancollection.data.FigureRepository
import com.garyfimo.batmancollection.data.FigureType
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
class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository: FigureRepository = mockk()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `emits Ready state when repository returns Success`() = runTest(dispatcher) {
        val items = listOf(
            Figure(
                id = "funko-the-batman-1187",
                tipo = FigureType.Funko,
                nombre = "The Batman",
                serie = "1187",
                imageUrl = "https://example.com/1187.jpg",
            ),
        )
        coEvery { repository.list() } returns FigureListResult.Success(items)

        val viewModel = HomeViewModel(repository)

        viewModel.state.test {
            assertEquals(HomeUiState.Loading, awaitItem())
            assertEquals(HomeUiState.Ready(items), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setFilter narrows visibleItems to the matching tipo`() = runTest(dispatcher) {
        val funko = Figure("f1", FigureType.Funko, "The Batman", "1187", "")
        val hw = Figure("h1", FigureType.HotWheels, "Batmobile", "103/250", "")
        val otros = Figure("o1", FigureType.Otros, "Marcelo Wong", "-", "")
        val items = listOf(funko, hw, otros)
        coEvery { repository.list() } returns FigureListResult.Success(items)

        val viewModel = HomeViewModel(repository)

        viewModel.state.test {
            assertEquals(HomeUiState.Loading, awaitItem())
            val ready = awaitItem() as HomeUiState.Ready
            assertEquals(items, ready.visibleItems)

            viewModel.setFilter(FigureType.HotWheels)
            val filtered = awaitItem() as HomeUiState.Ready
            assertEquals(listOf(hw), filtered.visibleItems)
            assertEquals(FigureType.HotWheels, filtered.filter)

            viewModel.setFilter(null)
            val cleared = awaitItem() as HomeUiState.Ready
            assertEquals(items, cleared.visibleItems)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Error state when repository returns Failure`() = runTest(dispatcher) {
        coEvery { repository.list() } returns
            FigureListResult.Failure(IllegalStateException("boom"))

        val viewModel = HomeViewModel(repository)

        viewModel.state.test {
            assertEquals(HomeUiState.Loading, awaitItem())
            val error = awaitItem()
            assertTrue(error is HomeUiState.Error)
            assertEquals("boom", (error as HomeUiState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
