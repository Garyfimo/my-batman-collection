package com.garyfimo.batmancollection.figure.presentation.home

import app.cash.turbine.test
import com.garyfimo.batmancollection.analytics.domain.Analytics
import com.garyfimo.batmancollection.login.domain.AuthRepository
import com.garyfimo.batmancollection.login.domain.AuthState
import com.garyfimo.batmancollection.figure.domain.Figure
import com.garyfimo.batmancollection.figure.domain.FigureListResult
import com.garyfimo.batmancollection.figure.domain.FigureRemoveResult
import com.garyfimo.batmancollection.figure.domain.FigureRepository
import com.garyfimo.batmancollection.figure.domain.FigureType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
    private val authRepository: AuthRepository = mockk<AuthRepository>().also {
        every { it.state } returns MutableStateFlow<AuthState>(AuthState.SignedOut)
    }
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
        every { repository.observe() } returns flowOf(FigureListResult.Success(items))

        val viewModel = HomeViewModel(repository, authRepository, analytics)

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
        every { repository.observe() } returns flowOf(FigureListResult.Success(items))

        val viewModel = HomeViewModel(repository, authRepository, analytics)

        viewModel.state.test {
            assertEquals(HomeUiState.Loading, awaitItem())
            val ready = awaitItem() as HomeUiState.Ready
            assertEquals(items, ready.visibleItems)

            viewModel.onIntent(HomeIntent.FilterChanged(FigureType.HotWheels))
            val filtered = awaitItem() as HomeUiState.Ready
            assertEquals(listOf(hw), filtered.visibleItems)
            assertEquals(FigureType.HotWheels, filtered.filter)

            viewModel.onIntent(HomeIntent.FilterChanged(null))
            val cleared = awaitItem() as HomeUiState.Ready
            assertEquals(items, cleared.visibleItems)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Error state when repository returns Failure`() = runTest(dispatcher) {
        every { repository.observe() } returns
            flowOf(FigureListResult.Failure(IllegalStateException("boom")))

        val viewModel = HomeViewModel(repository, authRepository, analytics)

        viewModel.state.test {
            assertEquals(HomeUiState.Loading, awaitItem())
            val error = awaitItem()
            assertTrue(error is HomeUiState.Error)
            assertEquals("boom", (error as HomeUiState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `default sort preserves the repository's input order`() = runTest(dispatcher) {
        val b = Figure("1", FigureType.Funko, "Batman", "1187", "")
        val a = Figure("2", FigureType.Funko, "Alfred", "202", "")
        val c = Figure("3", FigureType.Funko, "Catwoman", "203", "")
        // Repo returns these in a specific order; CreatedAt sort must not re-sort.
        every { repository.observe() } returns flowOf(FigureListResult.Success(listOf(b, a, c)))

        val viewModel = HomeViewModel(repository, authRepository, analytics)

        viewModel.state.test {
            assertEquals(HomeUiState.Loading, awaitItem())
            val ready = awaitItem() as HomeUiState.Ready
            assertEquals(listOf(b, a, c), ready.visibleItems)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setSort Name orders alphabetically ignoring case`() = runTest(dispatcher) {
        val items = listOf(
            Figure("1", FigureType.Funko, "zatanna", "1", ""),
            Figure("2", FigureType.Funko, "Batman", "2", ""),
            Figure("3", FigureType.Funko, "alfred", "3", ""),
        )
        every { repository.observe() } returns flowOf(FigureListResult.Success(items))

        val viewModel = HomeViewModel(repository, authRepository, analytics)

        viewModel.state.test {
            assertEquals(HomeUiState.Loading, awaitItem())
            awaitItem()
            viewModel.onIntent(HomeIntent.SortChanged(HomeSort.Name))
            val ready = awaitItem() as HomeUiState.Ready
            assertEquals(listOf("alfred", "Batman", "zatanna"), ready.visibleItems.map { it.nombre })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setSort Type groups by tipo and then by name`() = runTest(dispatcher) {
        val items = listOf(
            Figure("1", FigureType.Otros, "Wong", "-", ""),
            Figure("2", FigureType.Funko, "Batman", "1187", ""),
            Figure("3", FigureType.HotWheels, "Batmobile", "103/250", ""),
            Figure("4", FigureType.Funko, "Alfred", "202", ""),
        )
        every { repository.observe() } returns flowOf(FigureListResult.Success(items))

        val viewModel = HomeViewModel(repository, authRepository, analytics)

        viewModel.state.test {
            assertEquals(HomeUiState.Loading, awaitItem())
            awaitItem()
            viewModel.onIntent(HomeIntent.SortChanged(HomeSort.Type))
            val ready = awaitItem() as HomeUiState.Ready
            assertEquals(
                listOf("Alfred", "Batman", "Batmobile", "Wong"),
                ready.visibleItems.map { it.nombre },
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setSort Serie orders alphabetically`() = runTest(dispatcher) {
        val items = listOf(
            Figure("1", FigureType.Funko, "A", "zeta", ""),
            Figure("2", FigureType.Funko, "B", "alpha", ""),
            Figure("3", FigureType.Funko, "C", "Beta", ""),
        )
        every { repository.observe() } returns flowOf(FigureListResult.Success(items))

        val viewModel = HomeViewModel(repository, authRepository, analytics)

        viewModel.state.test {
            assertEquals(HomeUiState.Loading, awaitItem())
            awaitItem()
            viewModel.onIntent(HomeIntent.SortChanged(HomeSort.Serie))
            val ready = awaitItem() as HomeUiState.Ready
            assertEquals(listOf("alpha", "Beta", "zeta"), ready.visibleItems.map { it.serie })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `remove delegates to repository and emits nothing on success`() = runTest(dispatcher) {
        every { repository.observe() } returns flowOf(FigureListResult.Success(emptyList()))
        coEvery { repository.remove("abc") } returns FigureRemoveResult.Success

        val viewModel = HomeViewModel(repository, authRepository, analytics)

        viewModel.effects.test {
            viewModel.onIntent(HomeIntent.FigureRemoved(Figure("abc", FigureType.Funko, "X", "1", "")))
            advanceUntilIdle()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { repository.remove("abc") }
    }

    @Test
    fun `setQuery filters by nombre case-insensitively`() = runTest(dispatcher) {
        val batman = Figure("f1", FigureType.Funko, "The Batman", "1187", "")
        val joker = Figure("f2", FigureType.Funko, "The Joker", "202", "")
        every { repository.observe() } returns
            flowOf(FigureListResult.Success(listOf(batman, joker)))

        val viewModel = HomeViewModel(repository, authRepository, analytics)

        viewModel.state.test {
            assertEquals(HomeUiState.Loading, awaitItem())
            awaitItem() // Ready(all)
            viewModel.onIntent(HomeIntent.QueryChanged("batman"))
            val ready = awaitItem() as HomeUiState.Ready
            assertEquals(listOf(batman), ready.visibleItems)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setQuery also matches against serie`() = runTest(dispatcher) {
        val batman = Figure("f1", FigureType.Funko, "The Batman", "1187", "")
        val mobile = Figure("h1", FigureType.HotWheels, "Batmobile", "103/250", "")
        every { repository.observe() } returns
            flowOf(FigureListResult.Success(listOf(batman, mobile)))

        val viewModel = HomeViewModel(repository, authRepository, analytics)

        viewModel.state.test {
            assertEquals(HomeUiState.Loading, awaitItem())
            awaitItem()
            viewModel.onIntent(HomeIntent.QueryChanged("103"))
            val ready = awaitItem() as HomeUiState.Ready
            assertEquals(listOf(mobile), ready.visibleItems)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setQuery combines with tipo filter`() = runTest(dispatcher) {
        val funkoBatman = Figure("f1", FigureType.Funko, "Batman", "1187", "")
        val hwBatman = Figure("h1", FigureType.HotWheels, "Batmobile", "103/250", "")
        val hwOther = Figure("h2", FigureType.HotWheels, "Mustang", "77", "")
        every { repository.observe() } returns
            flowOf(FigureListResult.Success(listOf(funkoBatman, hwBatman, hwOther)))

        val viewModel = HomeViewModel(repository, authRepository, analytics)

        viewModel.state.test {
            assertEquals(HomeUiState.Loading, awaitItem())
            awaitItem()
            viewModel.onIntent(HomeIntent.FilterChanged(FigureType.HotWheels))
            awaitItem()
            viewModel.onIntent(HomeIntent.QueryChanged("bat"))
            val ready = awaitItem() as HomeUiState.Ready
            assertEquals(listOf(hwBatman), ready.visibleItems)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `remove emits RemoveFailed when repository fails`() = runTest(dispatcher) {
        every { repository.observe() } returns flowOf(FigureListResult.Success(emptyList()))
        coEvery { repository.remove("abc") } returns
            FigureRemoveResult.Failure(IllegalStateException("nope"))

        val viewModel = HomeViewModel(repository, authRepository, analytics)

        viewModel.effects.test {
            viewModel.onIntent(HomeIntent.FigureRemoved(Figure("abc", FigureType.Funko, "X", "1", "")))
            advanceUntilIdle()
            assertEquals(HomeEffect.RemoveFailed("nope"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
