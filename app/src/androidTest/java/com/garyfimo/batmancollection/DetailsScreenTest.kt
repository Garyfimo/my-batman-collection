package com.garyfimo.batmancollection

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.garyfimo.batmancollection.data.Batman
import com.garyfimo.batmancollection.data.BatmanLookup
import com.garyfimo.batmancollection.data.BatmanRepository
import com.garyfimo.batmancollection.ui.details.DetailsScreen
import com.garyfimo.batmancollection.ui.details.DetailsViewModel
import com.garyfimo.batmancollection.ui.details.TAG_DETAILS_CONTENT
import com.garyfimo.batmancollection.ui.details.TAG_DETAILS_ERROR
import com.garyfimo.batmancollection.ui.details.TAG_DETAILS_LOADING
import com.garyfimo.batmancollection.ui.details.TAG_DETAILS_NOT_FOUND
import com.garyfimo.batmancollection.ui.details.TAG_DETAILS_ROOT
import com.garyfimo.batmancollection.ui.theme.BatmanCollectionTheme
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DetailsScreenTest {

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

    private fun contentWith(viewModel: DetailsViewModel, onBack: () -> Unit = {}) {
        composeRule.setContent {
            BatmanCollectionTheme {
                DetailsScreen(
                    id = sampleBatman.id,
                    onBack = onBack,
                    viewModel = viewModel,
                )
            }
        }
    }

    @Test
    fun detailsScreen_showsLoading_whileRepositoryIsPending() {
        val repository: BatmanRepository = mockk()
        // Suspend forever so the Loading state is observable.
        val never = CompletableDeferred<BatmanLookup>()
        coEvery { repository.get(sampleBatman.id) } coAnswers { never.await() }

        contentWith(DetailsViewModel(id = sampleBatman.id, repository = repository))

        composeRule.onNodeWithTag(TAG_DETAILS_ROOT).assertIsDisplayed()
        composeRule.onNodeWithTag(TAG_DETAILS_LOADING).assertIsDisplayed()
    }

    @Test
    fun detailsScreen_showsReadyContent_whenFound() {
        val repository: BatmanRepository = mockk()
        coEvery { repository.get(sampleBatman.id) } returns BatmanLookup.Found(sampleBatman)

        contentWith(DetailsViewModel(id = sampleBatman.id, repository = repository))

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithText(sampleBatman.alterEgo)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag(TAG_DETAILS_CONTENT).assertIsDisplayed()
        composeRule.onNodeWithText(sampleBatman.alterEgo).assertIsDisplayed()
        composeRule.onNodeWithText(sampleBatman.era).assertIsDisplayed()
        composeRule.onNodeWithText(sampleBatman.firstAppearance).assertIsDisplayed()
        composeRule.onNodeWithText(sampleBatman.description).assertIsDisplayed()
    }

    @Test
    fun detailsScreen_showsNotFound_whenRepositoryHasNoMatch() {
        val repository: BatmanRepository = mockk()
        coEvery { repository.get(sampleBatman.id) } returns BatmanLookup.NotFound

        contentWith(DetailsViewModel(id = sampleBatman.id, repository = repository))

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithTag(TAG_DETAILS_NOT_FOUND)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag(TAG_DETAILS_NOT_FOUND).assertIsDisplayed()
    }

    @Test
    fun detailsScreen_showsError_whenRepositoryFails() {
        val repository: BatmanRepository = mockk()
        coEvery { repository.get(sampleBatman.id) } returns
            BatmanLookup.Failure(IllegalStateException("boom"))

        contentWith(DetailsViewModel(id = sampleBatman.id, repository = repository))

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("boom").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(TAG_DETAILS_ERROR).assertIsDisplayed()
    }

    @Test
    fun detailsScreen_backIcon_invokesOnBack() {
        val repository: BatmanRepository = mockk()
        coEvery { repository.get(sampleBatman.id) } returns BatmanLookup.Found(sampleBatman)
        var backInvoked = false

        contentWith(
            viewModel = DetailsViewModel(id = sampleBatman.id, repository = repository),
            onBack = { backInvoked = true },
        )

        composeRule.onNodeWithContentDescription("Back").performClick()
        assertTrue(backInvoked)
    }
}
