package com.garyfimo.batmancollection.ui.home

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
class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository: BatmanRepository = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `emits Ready state when repository returns items`() = runTest(dispatcher) {
        val items = listOf(
            Batman(
                id = "bruce-wayne",
                name = "Batman",
                alterEgo = "Bruce Wayne",
                era = "Modern Age",
                firstAppearance = "Detective Comics #27 (1939)",
                description = "The original Dark Knight.",
                imageUrl = "https://example.com/bruce.jpg",
            ),
        )
        coEvery { repository.list() } returns items

        val viewModel = HomeViewModel(repository)

        viewModel.state.test {
            assertEquals(HomeUiState.Loading, awaitItem())
            assertEquals(HomeUiState.Ready(items), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Error state when repository throws`() = runTest(dispatcher) {
        coEvery { repository.list() } throws IllegalStateException("boom")

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
