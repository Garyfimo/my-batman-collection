package com.garyfimo.batmancollection

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.garyfimo.batmancollection.data.Batman
import com.garyfimo.batmancollection.data.BatmanListResult
import com.garyfimo.batmancollection.data.BatmanRepository
import com.garyfimo.batmancollection.ui.home.HomeScreen
import com.garyfimo.batmancollection.ui.home.HomeViewModel
import com.garyfimo.batmancollection.ui.home.TAG_HOME_ITEM_PREFIX
import com.garyfimo.batmancollection.ui.home.TAG_HOME_OVERLAY
import com.garyfimo.batmancollection.ui.home.TAG_HOME_OVERLAY_IMAGE
import com.garyfimo.batmancollection.ui.home.TAG_HOME_ROOT
import com.garyfimo.batmancollection.ui.home.TAG_HOME_THUMB_PREFIX
import com.garyfimo.batmancollection.ui.theme.BatmanCollectionTheme
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        imageUrl = "https://example.com/bruce.jpg",
    )

    private fun fakeRepository(items: List<Batman> = listOf(sampleBatman)): BatmanRepository =
        mockk<BatmanRepository>().also {
            coEvery { it.list() } returns BatmanListResult.Success(items)
        }

    private fun waitForItem(headlineText: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(headlineText).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun homeScreen_rendersItemFromRepository() {
        composeRule.setContent {
            BatmanCollectionTheme {
                HomeScreen(
                    onItemClick = {},
                    viewModel = HomeViewModel(fakeRepository()),
                )
            }
        }

        composeRule.onNodeWithTag(TAG_HOME_ROOT).assertIsDisplayed()
        waitForItem("Bruce Wayne")
        composeRule.onNodeWithText("Bruce Wayne").assertIsDisplayed()
        composeRule.onNodeWithText("Modern Age").assertIsDisplayed()
    }

    @Test
    fun homeScreen_rowClick_emitsIdOnce() {
        var clickedId: String? = null

        composeRule.setContent {
            BatmanCollectionTheme {
                HomeScreen(
                    onItemClick = { clickedId = it },
                    viewModel = HomeViewModel(fakeRepository()),
                )
            }
        }

        waitForItem("Bruce Wayne")
        composeRule.onNodeWithTag("$TAG_HOME_ITEM_PREFIX${sampleBatman.id}").performClick()
        assertEquals(sampleBatman.id, clickedId)
    }

    @Test
    fun homeScreen_thumbnailTap_opensOverlay_withoutInvokingRowCallback() {
        var clickedId: String? = null

        composeRule.setContent {
            BatmanCollectionTheme {
                HomeScreen(
                    onItemClick = { clickedId = it },
                    viewModel = HomeViewModel(fakeRepository()),
                )
            }
        }

        waitForItem("Bruce Wayne")
        composeRule.onNodeWithTag(TAG_HOME_OVERLAY).assertDoesNotExist()
        composeRule.onNodeWithTag("$TAG_HOME_THUMB_PREFIX${sampleBatman.id}").performClick()

        composeRule.onNodeWithTag(TAG_HOME_OVERLAY).assertIsDisplayed()
        composeRule.onNodeWithTag(TAG_HOME_OVERLAY_IMAGE).assertIsDisplayed()
        assertNull("row callback should not fire on image tap", clickedId)
    }

    @Test
    fun homeScreen_scrimTap_dismissesOverlay() {
        composeRule.setContent {
            BatmanCollectionTheme {
                HomeScreen(
                    onItemClick = {},
                    viewModel = HomeViewModel(fakeRepository()),
                )
            }
        }

        waitForItem("Bruce Wayne")
        composeRule.onNodeWithTag("$TAG_HOME_THUMB_PREFIX${sampleBatman.id}").performClick()
        composeRule.onNodeWithTag(TAG_HOME_OVERLAY).assertIsDisplayed()

        composeRule.onNodeWithTag(TAG_HOME_OVERLAY).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Bruce Wayne").fetchSemanticsNodes().isNotEmpty() &&
                composeRule
                    .onAllNodesWithTag(TAG_HOME_OVERLAY)
                    .fetchSemanticsNodes()
                    .isEmpty()
        }
        composeRule.onNodeWithTag(TAG_HOME_OVERLAY).assertDoesNotExist()
    }
}
