package com.garyfimo.batmancollection.wishlist.presentation.wishlist

import app.cash.turbine.test
import com.garyfimo.batmancollection.analytics.domain.Analytics
import com.garyfimo.batmancollection.analytics.domain.Events
import com.garyfimo.batmancollection.core.domain.FigureType
import com.garyfimo.batmancollection.login.domain.AuthRepository
import com.garyfimo.batmancollection.login.domain.AuthState
import com.garyfimo.batmancollection.wishlist.domain.WishItem
import com.garyfimo.batmancollection.wishlist.domain.WishListResult
import com.garyfimo.batmancollection.wishlist.domain.WishRemoveResult
import com.garyfimo.batmancollection.wishlist.domain.WishRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
class WishlistViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository: WishRepository = mockk(relaxed = true)
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

    private val sampleWish = WishItem(
        id = "wish-1",
        tipo = FigureType.Funko,
        nombre = "The Batman",
        serie = "1187",
        url = "https://example.com/batman",
    )

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `fires screen_view on init`() = runTest(dispatcher) {
        every { repository.observe() } returns flowOf(WishListResult.Success(emptyList()))

        WishlistViewModel(repository, authRepository, analytics)
        advanceUntilIdle()

        assertTrue(analytics.events.any { it.first == Events.SCREEN_VIEW })
    }

    @Test
    fun `emits Ready state when repository returns Success`() = runTest(dispatcher) {
        every { repository.observe() } returns flowOf(WishListResult.Success(listOf(sampleWish)))

        val viewModel = WishlistViewModel(repository, authRepository, analytics)

        viewModel.state.test {
            assertEquals(WishlistUiState.Loading, awaitItem())
            assertEquals(WishlistUiState.Ready(listOf(sampleWish)), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Error state when repository returns Failure`() = runTest(dispatcher) {
        every { repository.observe() } returns
            flowOf(WishListResult.Failure(IllegalStateException("boom")))

        val viewModel = WishlistViewModel(repository, authRepository, analytics)

        viewModel.state.test {
            assertEquals(WishlistUiState.Loading, awaitItem())
            assertEquals(WishlistUiState.Error("boom"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `WishSelected with non-blank url emits OpenUrl and tracks select_content`() = runTest(dispatcher) {
        every { repository.observe() } returns flowOf(WishListResult.Success(listOf(sampleWish)))

        val viewModel = WishlistViewModel(repository, authRepository, analytics)

        viewModel.effects.test {
            viewModel.onIntent(WishlistIntent.WishSelected(sampleWish))
            advanceUntilIdle()
            assertEquals(WishlistEffect.OpenUrl(sampleWish.url), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(analytics.events.any { it.first == Events.SELECT_CONTENT })
    }

    @Test
    fun `WishSelected with blank url does not emit OpenUrl but still tracks`() = runTest(dispatcher) {
        every { repository.observe() } returns flowOf(WishListResult.Success(emptyList()))
        val blankUrlWish = sampleWish.copy(url = "")

        val viewModel = WishlistViewModel(repository, authRepository, analytics)

        viewModel.effects.test {
            viewModel.onIntent(WishlistIntent.WishSelected(blankUrlWish))
            advanceUntilIdle()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(analytics.events.any { it.first == Events.SELECT_CONTENT })
    }

    @Test
    fun `WishRemoved success tracks item_deleted and does not emit effect`() = runTest(dispatcher) {
        every { repository.observe() } returns flowOf(WishListResult.Success(emptyList()))
        coEvery { repository.remove(sampleWish.id) } returns WishRemoveResult.Success

        val viewModel = WishlistViewModel(repository, authRepository, analytics)

        viewModel.effects.test {
            viewModel.onIntent(WishlistIntent.WishRemoved(sampleWish))
            advanceUntilIdle()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { repository.remove(sampleWish.id) }
        assertTrue(analytics.events.any { it.first == Events.ITEM_DELETED })
    }

    @Test
    fun `WishRemoved failure emits RemoveFailed effect`() = runTest(dispatcher) {
        every { repository.observe() } returns flowOf(WishListResult.Success(emptyList()))
        coEvery { repository.remove(sampleWish.id) } returns
            WishRemoveResult.Failure(IllegalStateException("nope"))

        val viewModel = WishlistViewModel(repository, authRepository, analytics)

        viewModel.effects.test {
            viewModel.onIntent(WishlistIntent.WishRemoved(sampleWish))
            advanceUntilIdle()
            assertEquals(WishlistEffect.RemoveFailed("nope"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `authState mirrors AuthRepository state`() = runTest(dispatcher) {
        every { repository.observe() } returns flowOf(WishListResult.Success(emptyList()))
        every { authRepository.state } returns
            MutableStateFlow<AuthState>(AuthState.SignedIn("garyfimo@gmail.com"))

        val viewModel = WishlistViewModel(repository, authRepository, analytics)

        assertEquals(AuthState.SignedIn("garyfimo@gmail.com"), viewModel.authState.value)
    }
}
