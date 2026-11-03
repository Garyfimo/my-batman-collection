package com.garyfimo.batmancollection

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.garyfimo.batmancollection.data.Figure
import com.garyfimo.batmancollection.data.FigureListResult
import com.garyfimo.batmancollection.data.FigureLookup
import com.garyfimo.batmancollection.data.AuthRepository
import com.garyfimo.batmancollection.data.AuthState
import com.garyfimo.batmancollection.data.FigureRepository
import com.garyfimo.batmancollection.data.FigureType
import com.garyfimo.batmancollection.navigation.BatmanNavDisplay
import com.garyfimo.batmancollection.ui.addfigure.AddFigureViewModel
import com.garyfimo.batmancollection.ui.details.DetailsViewModel
import com.garyfimo.batmancollection.ui.details.TAG_DETAILS_CONTENT
import com.garyfimo.batmancollection.ui.home.HomeViewModel
import com.garyfimo.batmancollection.ui.home.TAG_HOME_ITEM_PREFIX
import com.garyfimo.batmancollection.ui.home.TAG_HOME_ROOT
import com.garyfimo.batmancollection.ui.theme.BatmanCollectionTheme
import com.garyfimo.batmancollection.ui.login.LoginViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

class BatmanNavDisplayTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val bruce = Figure(
        id = "funko-the-batman-1187",
        tipo = FigureType.Funko,
        nombre = "The Batman",
        serie = "1187",
        imageUrl = "https://example.com/1187.jpg",
    )
    private val mobile = Figure(
        id = "hot-wheels-batmobile-103-250-3-5",
        tipo = FigureType.HotWheels,
        nombre = "Batmobile",
        serie = "103/250 (3/5)",
        imageUrl = "https://example.com/batmobile.jpg",
    )

    // Replace whatever BatmanApp.onCreate started with a test-only Koin instance so
    // koinViewModel() always resolves the mocked repository, no matter whether it
    // reads from the LocalKoinApplication composable scope or the global one.
    @Before
    fun setUp() {
        runCatching { stopKoin() }
        val repository: FigureRepository = mockk<FigureRepository>().also {
            every { it.observe() } returns flowOf(FigureListResult.Success(listOf(bruce, mobile)))
            coEvery { it.get(bruce.id) } returns FigureLookup.Found(bruce)
            coEvery { it.get(mobile.id) } returns FigureLookup.Found(mobile)
        }
        startKoin {
            modules(
                module {
                    single<FigureRepository> { repository }
                    single<AuthRepository> {
                        mockk<AuthRepository>().also {
                            every { it.state } returns MutableStateFlow<AuthState>(AuthState.SignedOut)
                        }
                    }
                    viewModelOf(::HomeViewModel)
                    viewModel { (id: String) -> DetailsViewModel(id = id, repository = get()) }
                    viewModelOf(::AddFigureViewModel)
                    viewModelOf(::LoginViewModel)
                },
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun start() {
        composeRule.setContent {
            BatmanCollectionTheme {
                BatmanNavDisplay()
            }
        }
    }

    @Test
    fun navDisplay_startsOnHome_withBothItems() {
        start()

        composeRule.onNodeWithTag(TAG_HOME_ROOT).assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(bruce.nombre).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(bruce.nombre).assertIsDisplayed()
        composeRule.onNodeWithText(mobile.nombre).assertIsDisplayed()
    }

    @Test
    fun navDisplay_tappingRow_navigatesToDetail_forThatId() {
        start()
        composeRule.mainClock.autoAdvance = true
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(mobile.nombre).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("$TAG_HOME_ITEM_PREFIX${mobile.id}").performClick()
        // Give the Nav3 enter animation and the DetailsViewModel coroutine time to
        // settle on v2's StandardTestDispatcher-backed clock.
        composeRule.mainClock.advanceTimeBy(1_000L)
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithTag(TAG_DETAILS_CONTENT)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag(TAG_DETAILS_CONTENT).assertIsDisplayed()
        composeRule.onNodeWithText(mobile.serie).assertIsDisplayed()
    }

    @Test
    fun navDisplay_backFromDetail_returnsToList() {
        start()
        composeRule.mainClock.autoAdvance = true
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(bruce.nombre).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("$TAG_HOME_ITEM_PREFIX${bruce.id}").performClick()
        composeRule.mainClock.advanceTimeBy(1_000L)
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithTag(TAG_DETAILS_CONTENT)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.mainClock.advanceTimeBy(1_000L)
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithTag(TAG_HOME_ROOT)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag(TAG_HOME_ROOT).assertIsDisplayed()
    }
}
