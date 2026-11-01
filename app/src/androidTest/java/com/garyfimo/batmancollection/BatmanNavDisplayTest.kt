package com.garyfimo.batmancollection

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.garyfimo.batmancollection.data.Batman
import com.garyfimo.batmancollection.data.BatmanListResult
import com.garyfimo.batmancollection.data.BatmanLookup
import com.garyfimo.batmancollection.data.BatmanRepository
import com.garyfimo.batmancollection.navigation.BatmanNavDisplay
import com.garyfimo.batmancollection.ui.details.DetailsViewModel
import com.garyfimo.batmancollection.ui.details.TAG_DETAILS_CONTENT
import com.garyfimo.batmancollection.ui.home.HomeViewModel
import com.garyfimo.batmancollection.ui.home.TAG_HOME_ITEM_PREFIX
import com.garyfimo.batmancollection.ui.home.TAG_HOME_ROOT
import com.garyfimo.batmancollection.ui.theme.BatmanCollectionTheme
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinApplication
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

class BatmanNavDisplayTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val bruce = Batman(
        id = "bruce-wayne",
        name = "Batman",
        alterEgo = "Bruce Wayne",
        era = "Modern Age",
        firstAppearance = "Detective Comics #27 (1939)",
        description = "The original Dark Knight.",
        imageUrl = "https://example.com/bruce.jpg",
    )
    private val dick = Batman(
        id = "dick-grayson",
        name = "Batman",
        alterEgo = "Dick Grayson",
        era = "Battle for the Cowl",
        firstAppearance = "Battle for the Cowl #3 (2009)",
        description = "The first Robin takes up the mantle.",
        imageUrl = "https://example.com/dick.jpg",
    )

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun fakeRepository(): BatmanRepository = mockk<BatmanRepository>().also {
        coEvery { it.list() } returns BatmanListResult.Success(listOf(bruce, dick))
        coEvery { it.get(bruce.id) } returns BatmanLookup.Found(bruce)
        coEvery { it.get(dick.id) } returns BatmanLookup.Found(dick)
    }

    @Suppress("DEPRECATION") // koinConfiguration DSL was added in Koin > 4.0.1
    private fun start(repository: BatmanRepository = fakeRepository()) {
        composeRule.setContent {
            KoinApplication(
                application = {
                    modules(
                        module {
                            single<BatmanRepository> { repository }
                            viewModelOf(::HomeViewModel)
                            viewModel { (id: String) ->
                                DetailsViewModel(id = id, repository = get())
                            }
                        },
                    )
                },
            ) {
                BatmanCollectionTheme {
                    BatmanNavDisplay()
                }
            }
        }
    }

    @Test
    fun navDisplay_startsOnHome_withBothItems() {
        start()

        composeRule.onNodeWithTag(TAG_HOME_ROOT).assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(bruce.alterEgo).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(bruce.alterEgo).assertIsDisplayed()
        composeRule.onNodeWithText(dick.alterEgo).assertIsDisplayed()
    }

    @Test
    fun navDisplay_tappingRow_navigatesToDetail_forThatId() {
        start()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(dick.alterEgo).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("$TAG_HOME_ITEM_PREFIX${dick.id}").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithText(dick.firstAppearance)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag(TAG_DETAILS_CONTENT).assertIsDisplayed()
        composeRule.onNodeWithText(dick.firstAppearance).assertIsDisplayed()
    }

    @Test
    fun navDisplay_backFromDetail_returnsToList() {
        start()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(bruce.alterEgo).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("$TAG_HOME_ITEM_PREFIX${bruce.id}").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithText(bruce.firstAppearance)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithText(bruce.alterEgo)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag(TAG_HOME_ROOT).assertIsDisplayed()
    }
}
