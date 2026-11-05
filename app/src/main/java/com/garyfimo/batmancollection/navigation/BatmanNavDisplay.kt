package com.garyfimo.batmancollection.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.garyfimo.batmancollection.figure.presentation.LocalSharedTransitionScope
import com.garyfimo.batmancollection.figure.presentation.addfigure.AddFigureScreen
import com.garyfimo.batmancollection.figure.presentation.details.DetailsScreen
import com.garyfimo.batmancollection.figure.presentation.home.HomeScreen
import com.garyfimo.batmancollection.login.presentation.LoginScreen

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BatmanNavDisplay() {
    val backStack = rememberNavBackStack(HomeKey)

    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryProvider = entryProvider {
                    entry<HomeKey> {
                        HomeScreen(
                            onItemClick = { id -> backStack.add(DetailsKey(id)) },
                            onAddFigure = { backStack.add(AddFigureKey) },
                            onSignIn = { backStack.add(LoginKey) },
                        )
                    }
                    entry<DetailsKey> { key ->
                        DetailsScreen(
                            id = key.id,
                            onBack = { backStack.removeLastOrNull() },
                        )
                    }
                    entry<AddFigureKey> {
                        AddFigureScreen(
                            onBack = { backStack.removeLastOrNull() },
                        )
                    }
                    entry<LoginKey> {
                        LoginScreen(
                            onBack = { backStack.removeLastOrNull() },
                        )
                    }
                },
            )
        }
    }
}
