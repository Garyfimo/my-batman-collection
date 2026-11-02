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
import com.garyfimo.batmancollection.data.FigureLookup
import com.garyfimo.batmancollection.data.FigureRepository
import com.garyfimo.batmancollection.data.FigureType
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

    private val sampleFigure = Figure(
        id = "funko-the-batman-1187",
        tipo = FigureType.Funko,
        nombre = "The Batman",
        serie = "1187",
        imageUrl = "https://example.com/1187.jpg",
    )

    private fun contentWith(viewModel: DetailsViewModel, onBack: () -> Unit = {}) {
        composeRule.setContent {
            BatmanCollectionTheme {
                DetailsScreen(
                    id = sampleFigure.id,
                    onBack = onBack,
                    viewModel = viewModel,
                )
            }
        }
    }

    @Test
    fun detailsScreen_showsLoading_whileRepositoryIsPending() {
        val repository: FigureRepository = mockk()
        val never = CompletableDeferred<FigureLookup>()
        coEvery { repository.get(sampleFigure.id) } coAnswers { never.await() }

        contentWith(DetailsViewModel(id = sampleFigure.id, repository = repository))

        composeRule.onNodeWithTag(TAG_DETAILS_ROOT).assertIsDisplayed()
        composeRule.onNodeWithTag(TAG_DETAILS_LOADING).assertIsDisplayed()
    }

    @Test
    fun detailsScreen_showsReadyContent_whenFound() {
        val repository: FigureRepository = mockk()
        coEvery { repository.get(sampleFigure.id) } returns FigureLookup.Found(sampleFigure)

        contentWith(DetailsViewModel(id = sampleFigure.id, repository = repository))

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithText(sampleFigure.serie)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag(TAG_DETAILS_CONTENT).assertIsDisplayed()
        composeRule.onNodeWithText(sampleFigure.nombre).assertIsDisplayed()
        composeRule.onNodeWithText(sampleFigure.tipo.label).assertIsDisplayed()
        composeRule.onNodeWithText(sampleFigure.serie).assertIsDisplayed()
    }

    @Test
    fun detailsScreen_showsNotFound_whenRepositoryHasNoMatch() {
        val repository: FigureRepository = mockk()
        coEvery { repository.get(sampleFigure.id) } returns FigureLookup.NotFound

        contentWith(DetailsViewModel(id = sampleFigure.id, repository = repository))

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
        val repository: FigureRepository = mockk()
        coEvery { repository.get(sampleFigure.id) } returns
            FigureLookup.Failure(IllegalStateException("boom"))

        contentWith(DetailsViewModel(id = sampleFigure.id, repository = repository))

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("boom").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(TAG_DETAILS_ERROR).assertIsDisplayed()
    }

    @Test
    fun detailsScreen_backIcon_invokesOnBack() {
        val repository: FigureRepository = mockk()
        coEvery { repository.get(sampleFigure.id) } returns FigureLookup.Found(sampleFigure)
        var backInvoked = false

        contentWith(
            viewModel = DetailsViewModel(id = sampleFigure.id, repository = repository),
            onBack = { backInvoked = true },
        )

        composeRule.onNodeWithContentDescription("Back").performClick()
        assertTrue(backInvoked)
    }
}
