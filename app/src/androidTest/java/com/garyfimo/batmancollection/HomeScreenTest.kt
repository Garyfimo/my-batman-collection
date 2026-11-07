package com.garyfimo.batmancollection

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.garyfimo.batmancollection.analytics.domain.Analytics
import com.garyfimo.batmancollection.login.domain.AuthRepository
import com.garyfimo.batmancollection.login.domain.AuthState
import com.garyfimo.batmancollection.figure.domain.Figure
import com.garyfimo.batmancollection.figure.domain.FigureListResult
import com.garyfimo.batmancollection.figure.domain.FigureRepository
import com.garyfimo.batmancollection.core.domain.FigureType
import com.garyfimo.batmancollection.figure.presentation.home.HomeScreen
import com.garyfimo.batmancollection.figure.presentation.home.HomeViewModel
import com.garyfimo.batmancollection.figure.presentation.home.TAG_HOME_ITEM_PREFIX
import com.garyfimo.batmancollection.figure.presentation.home.TAG_HOME_OVERLAY
import com.garyfimo.batmancollection.figure.presentation.home.TAG_HOME_OVERLAY_IMAGE
import com.garyfimo.batmancollection.figure.presentation.home.TAG_HOME_ROOT
import com.garyfimo.batmancollection.figure.presentation.home.TAG_HOME_THUMB_PREFIX
import com.garyfimo.batmancollection.ui.theme.BatmanCollectionTheme
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleFigure = Figure(
        id = "funko-the-batman-1187",
        tipo = FigureType.Funko,
        nombre = "The Batman",
        serie = "1187",
        imageUrl = "https://example.com/1187.jpg",
    )

    private fun fakeRepository(items: List<Figure> = listOf(sampleFigure)): FigureRepository =
        mockk<FigureRepository>(relaxed = true).also {
            every { it.observe() } returns flowOf(FigureListResult.Success(items))
        }

    private fun fakeAuth(state: AuthState = AuthState.SignedOut): AuthRepository =
        mockk<AuthRepository>().also { every { it.state } returns MutableStateFlow(state) }

    private val recordingAnalytics = object : Analytics {
        override fun track(event: String, params: Map<String, Any>) = Unit
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
                    viewModel = HomeViewModel(fakeRepository(), fakeAuth(), recordingAnalytics),
                )
            }
        }

        composeRule.onNodeWithTag(TAG_HOME_ROOT).assertIsDisplayed()
        waitForItem(sampleFigure.nombre)
        composeRule.onNodeWithText(sampleFigure.nombre).assertIsDisplayed()
        // tipo.label renders twice: once as a FilterChip, once as the list row overline.
        composeRule.onAllNodesWithText(sampleFigure.tipo.label).assertCountEquals(2)
        composeRule.onNodeWithText(sampleFigure.serie).assertIsDisplayed()
    }

    @Test
    fun homeScreen_rowClick_emitsIdOnce() {
        var clickedId: String? = null

        composeRule.setContent {
            BatmanCollectionTheme {
                HomeScreen(
                    onItemClick = { clickedId = it },
                    viewModel = HomeViewModel(fakeRepository(), fakeAuth(), recordingAnalytics),
                )
            }
        }

        waitForItem(sampleFigure.nombre)
        composeRule.onNodeWithTag("$TAG_HOME_ITEM_PREFIX${sampleFigure.id}").performClick()
        assertEquals(sampleFigure.id, clickedId)
    }

    @Test
    fun homeScreen_thumbnailTap_opensOverlay_withoutInvokingRowCallback() {
        var clickedId: String? = null

        composeRule.setContent {
            BatmanCollectionTheme {
                HomeScreen(
                    onItemClick = { clickedId = it },
                    viewModel = HomeViewModel(fakeRepository(), fakeAuth(), recordingAnalytics),
                )
            }
        }

        waitForItem(sampleFigure.nombre)
        composeRule.onNodeWithTag(TAG_HOME_OVERLAY).assertDoesNotExist()
        composeRule
            .onNodeWithTag("$TAG_HOME_THUMB_PREFIX${sampleFigure.id}", useUnmergedTree = true)
            .performClick()

        composeRule.onNodeWithTag(TAG_HOME_OVERLAY).assertIsDisplayed()
        composeRule
            .onNodeWithTag(TAG_HOME_OVERLAY_IMAGE, useUnmergedTree = true)
            .assertIsDisplayed()
        assertNull("row callback should not fire on image tap", clickedId)
    }

    @Test
    fun homeScreen_scrimTap_dismissesOverlay() {
        composeRule.setContent {
            BatmanCollectionTheme {
                HomeScreen(
                    onItemClick = {},
                    viewModel = HomeViewModel(fakeRepository(), fakeAuth(), recordingAnalytics),
                )
            }
        }

        waitForItem(sampleFigure.nombre)
        composeRule
            .onNodeWithTag("$TAG_HOME_THUMB_PREFIX${sampleFigure.id}", useUnmergedTree = true)
            .performClick()
        composeRule.onNodeWithTag(TAG_HOME_OVERLAY).assertIsDisplayed()

        composeRule.onNodeWithTag(TAG_HOME_OVERLAY).performTouchInput { click(topLeft) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TAG_HOME_OVERLAY).assertDoesNotExist()
    }
}
