package com.garyfimo.batmancollection

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.garyfimo.batmancollection.analytics.domain.Analytics
import com.garyfimo.batmancollection.core.domain.FigureType
import com.garyfimo.batmancollection.login.domain.AuthRepository
import com.garyfimo.batmancollection.login.domain.AuthState
import com.garyfimo.batmancollection.ui.theme.BatmanCollectionTheme
import com.garyfimo.batmancollection.wishlist.domain.WishItem
import com.garyfimo.batmancollection.wishlist.domain.WishListResult
import com.garyfimo.batmancollection.wishlist.domain.WishRepository
import com.garyfimo.batmancollection.wishlist.presentation.wishlist.TAG_WISH_ADD_FAB
import com.garyfimo.batmancollection.wishlist.presentation.wishlist.TAG_WISH_EMPTY
import com.garyfimo.batmancollection.wishlist.presentation.wishlist.TAG_WISH_ROOT
import com.garyfimo.batmancollection.wishlist.presentation.wishlist.WishlistScreen
import com.garyfimo.batmancollection.wishlist.presentation.wishlist.WishlistViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

class WishlistScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleWish = WishItem(
        id = "wish-1",
        tipo = FigureType.Funko,
        nombre = "The Batman",
        serie = "1187",
        url = "https://example.com/batman",
    )

    private val recordingAnalytics = object : Analytics {
        override fun track(event: String, params: Map<String, Any>) = Unit
    }

    private fun fakeRepository(items: List<WishItem>): WishRepository =
        mockk<WishRepository>(relaxed = true).also {
            every { it.observe() } returns flowOf(WishListResult.Success(items))
        }

    private fun fakeAuth(state: AuthState): AuthRepository =
        mockk<AuthRepository>().also { every { it.state } returns MutableStateFlow(state) }

    private fun waitForItem(headlineText: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(headlineText).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun wishlistScreen_rendersEmptyState_whenRepositoryReturnsEmptyList() {
        composeRule.setContent {
            BatmanCollectionTheme {
                WishlistScreen(
                    onBack = {},
                    onAddWish = {},
                    viewModel = WishlistViewModel(
                        repository = fakeRepository(emptyList()),
                        authRepository = fakeAuth(AuthState.SignedOut),
                        analytics = recordingAnalytics,
                    ),
                )
            }
        }

        composeRule.onNodeWithTag(TAG_WISH_ROOT).assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Your wish list is empty").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(TAG_WISH_EMPTY).assertIsDisplayed()
    }

    @Test
    fun wishlistScreen_rendersItemFromRepository() {
        composeRule.setContent {
            BatmanCollectionTheme {
                WishlistScreen(
                    onBack = {},
                    onAddWish = {},
                    viewModel = WishlistViewModel(
                        repository = fakeRepository(listOf(sampleWish)),
                        authRepository = fakeAuth(AuthState.SignedOut),
                        analytics = recordingAnalytics,
                    ),
                )
            }
        }

        composeRule.onNodeWithTag(TAG_WISH_ROOT).assertIsDisplayed()
        waitForItem(sampleWish.nombre)
        composeRule.onNodeWithText(sampleWish.nombre).assertIsDisplayed()
        composeRule.onNodeWithText(sampleWish.tipo.label).assertIsDisplayed()
        composeRule.onNodeWithText(sampleWish.serie).assertIsDisplayed()
    }

    @Test
    fun wishlistScreen_hidesAddFab_whenSignedOut() {
        composeRule.setContent {
            BatmanCollectionTheme {
                WishlistScreen(
                    onBack = {},
                    onAddWish = {},
                    viewModel = WishlistViewModel(
                        repository = fakeRepository(emptyList()),
                        authRepository = fakeAuth(AuthState.SignedOut),
                        analytics = recordingAnalytics,
                    ),
                )
            }
        }

        composeRule.onNodeWithTag(TAG_WISH_ADD_FAB).assertDoesNotExist()
    }

    @Test
    fun wishlistScreen_hidesAddFab_whenSignedInAsNonOwner() {
        composeRule.setContent {
            BatmanCollectionTheme {
                WishlistScreen(
                    onBack = {},
                    onAddWish = {},
                    viewModel = WishlistViewModel(
                        repository = fakeRepository(emptyList()),
                        authRepository = fakeAuth(AuthState.SignedIn("someoneelse@example.com")),
                        analytics = recordingAnalytics,
                    ),
                )
            }
        }

        composeRule.onNodeWithTag(TAG_WISH_ADD_FAB).assertDoesNotExist()
    }

    @Test
    fun wishlistScreen_showsAddFab_whenSignedInAsOwner() {
        composeRule.setContent {
            BatmanCollectionTheme {
                WishlistScreen(
                    onBack = {},
                    onAddWish = {},
                    viewModel = WishlistViewModel(
                        repository = fakeRepository(emptyList()),
                        authRepository = fakeAuth(AuthState.SignedIn("garyfimo@gmail.com")),
                        analytics = recordingAnalytics,
                    ),
                )
            }
        }

        composeRule.onNodeWithTag(TAG_WISH_ADD_FAB).assertIsDisplayed()
    }
}
