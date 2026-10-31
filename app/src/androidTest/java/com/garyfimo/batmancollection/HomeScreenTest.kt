package com.garyfimo.batmancollection

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.garyfimo.batmancollection.data.Batman
import com.garyfimo.batmancollection.data.BatmanRepository
import com.garyfimo.batmancollection.ui.home.HomeScreen
import com.garyfimo.batmancollection.ui.home.HomeViewModel
import com.garyfimo.batmancollection.ui.home.TAG_HOME_ITEM_PREFIX
import com.garyfimo.batmancollection.ui.home.TAG_HOME_ROOT
import com.garyfimo.batmancollection.ui.theme.BatmanCollectionTheme
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleBatman = Batman(
        id = "bruce-wayne",
        name = "Batman",
        alterEgo = "Bruce Wayne",
        era = "Modern Age",
        firstAppearance = "Detective Comics #27 (1939)",
        description = "The original Dark Knight.",
    )

    @Test
    fun homeScreen_rendersItemFromRepository() {
        val repository: BatmanRepository = mockk()
        coEvery { repository.list() } returns listOf(sampleBatman)

        composeRule.setContent {
            BatmanCollectionTheme {
                HomeScreen(
                    onItemClick = {},
                    viewModel = HomeViewModel(repository),
                )
            }
        }

        composeRule.onNodeWithTag(TAG_HOME_ROOT).assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Bruce Wayne").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Bruce Wayne").assertIsDisplayed()
        composeRule.onNodeWithText("Modern Age").assertIsDisplayed()
    }

    @Test
    fun homeScreen_invokesCallbackWithIdOnItemClick() {
        val repository: BatmanRepository = mockk()
        coEvery { repository.list() } returns listOf(sampleBatman)
        var clickedId: String? = null

        composeRule.setContent {
            BatmanCollectionTheme {
                HomeScreen(
                    onItemClick = { clickedId = it },
                    viewModel = HomeViewModel(repository),
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithText("Bruce Wayne")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("$TAG_HOME_ITEM_PREFIX${sampleBatman.id}").performClick()

        assertEquals(sampleBatman.id, clickedId)
    }
}
