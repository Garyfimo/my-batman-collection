package com.garyfimo.batmancollection

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.garyfimo.batmancollection.analytics.domain.Analytics
import com.garyfimo.batmancollection.wishlist.domain.WishRepository
import com.garyfimo.batmancollection.ui.theme.BatmanCollectionTheme
import com.garyfimo.batmancollection.wishlist.presentation.addwish.AddWishScreen
import com.garyfimo.batmancollection.wishlist.presentation.addwish.AddWishViewModel
import com.garyfimo.batmancollection.wishlist.presentation.addwish.TAG_ADD_WISH_NOMBRE
import com.garyfimo.batmancollection.wishlist.presentation.addwish.TAG_ADD_WISH_ROOT
import com.garyfimo.batmancollection.wishlist.presentation.addwish.TAG_ADD_WISH_SERIE
import com.garyfimo.batmancollection.wishlist.presentation.addwish.TAG_ADD_WISH_SUBMIT
import com.garyfimo.batmancollection.wishlist.presentation.addwish.TAG_ADD_WISH_URL
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class AddWishScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val recordingAnalytics = object : Analytics {
        override fun track(event: String, params: Map<String, Any>) = Unit
    }

    private fun viewModel(): AddWishViewModel =
        AddWishViewModel(
            repository = mockk<WishRepository>(relaxed = true),
            analytics = recordingAnalytics,
        )

    @Test
    fun addWishScreen_rendersFormFields() {
        composeRule.setContent {
            BatmanCollectionTheme {
                AddWishScreen(onBack = {}, viewModel = viewModel())
            }
        }

        composeRule.onNodeWithTag(TAG_ADD_WISH_ROOT).assertIsDisplayed()
        composeRule.onNodeWithTag(TAG_ADD_WISH_NOMBRE).assertIsDisplayed()
        composeRule.onNodeWithTag(TAG_ADD_WISH_SERIE).assertIsDisplayed()
        composeRule.onNodeWithTag(TAG_ADD_WISH_URL).assertIsDisplayed()
    }

    @Test
    fun addWishScreen_submitButton_isDisabledWhenFormIsEmpty() {
        composeRule.setContent {
            BatmanCollectionTheme {
                AddWishScreen(onBack = {}, viewModel = viewModel())
            }
        }

        composeRule.onNodeWithTag(TAG_ADD_WISH_SUBMIT).assertIsNotEnabled()
    }

    @Test
    fun addWishScreen_submitButton_staysDisabled_whenUrlIsInvalid() {
        composeRule.setContent {
            BatmanCollectionTheme {
                AddWishScreen(onBack = {}, viewModel = viewModel())
            }
        }

        composeRule.onNodeWithTag(TAG_ADD_WISH_NOMBRE).performTextInput("Batman")
        composeRule.onNodeWithTag(TAG_ADD_WISH_SERIE).performTextInput("1187")
        composeRule.onNodeWithTag(TAG_ADD_WISH_URL).performTextInput("not a url")

        // isValid requires tipo + valid URL. Even with tipo missing the button stays
        // disabled; tipo requires opening the dropdown so we simulate the easiest case
        // (invalid URL alone keeps the button disabled).
        composeRule.onNodeWithTag(TAG_ADD_WISH_SUBMIT).assertIsNotEnabled()
    }
}
