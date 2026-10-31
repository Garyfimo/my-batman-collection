package com.garyfimo.batmancollection.ui.details

import app.cash.turbine.test
import com.garyfimo.batmancollection.data.Batman
import com.garyfimo.batmancollection.data.BatmanRepository
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
    private val repository: BatmanRepository = mockk()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `emits Ready when repository returns a Batman`() = runTest(dispatcher) {
        val batman = Batman(
            id = "bruce-wayne",
            name = "Batman",
            alterEgo = "Bruce Wayne",
            era = "Modern Age",
            firstAppearance = "Detective Comics #27 (1939)",
            description = "The original Dark Knight.",
            imageUrl = "https://example.com/bruce.jpg",
        )
        coEvery { repository.get("bruce-wayne") } returns batman

        val viewModel = DetailsViewModel(id = "bruce-wayne", repository = repository)

        viewModel.state.test {
            assertEquals(DetailsUiState.Loading, awaitItem())
            assertEquals(DetailsUiState.Ready(batman), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits NotFound when repository returns null`() = runTest(dispatcher) {
        coEvery { repository.get("ghost") } returns null

        val viewModel = DetailsViewModel(id = "ghost", repository = repository)

        viewModel.state.test {
            assertEquals(DetailsUiState.Loading, awaitItem())
            assertEquals(DetailsUiState.NotFound, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Error when repository throws`() = runTest(dispatcher) {
        coEvery { repository.get("bruce-wayne") } throws IllegalStateException("boom")

        val viewModel = DetailsViewModel(id = "bruce-wayne", repository = repository)

        viewModel.state.test {
            assertEquals(DetailsUiState.Loading, awaitItem())
            val error = awaitItem()
            assertTrue(error is DetailsUiState.Error)
            assertEquals("boom", (error as DetailsUiState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
