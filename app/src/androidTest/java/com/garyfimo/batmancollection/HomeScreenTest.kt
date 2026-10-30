package com.garyfimo.batmancollection

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.garyfimo.batmancollection.data.Batman
import com.garyfimo.batmancollection.data.BatmanRepository
import com.garyfimo.batmancollection.ui.home.HomeScreen
import com.garyfimo.batmancollection.ui.home.HomeViewModel
import com.garyfimo.batmancollection.ui.home.TAG_HOME_ROOT
import com.garyfimo.batmancollection.ui.theme.BatmanCollectionTheme
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeScreen_rendersItemFromRepository() {
        val repository: BatmanRepository = mockk()
        coEvery { repository.list() } returns listOf(
            Batman("1", "Bruce Wayne", "The Dark Knight"),
        )

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
    }
}
