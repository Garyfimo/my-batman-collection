package com.garyfimo.batmancollection.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.garyfimo.batmancollection.ui.details.DetailsScreen
import com.garyfimo.batmancollection.ui.home.HomeScreen

@Composable
fun BatmanNavDisplay() {
    val backStack = rememberNavBackStack(HomeKey)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<HomeKey> {
                HomeScreen(
                    onItemClick = { id -> backStack.add(DetailsKey(id)) },
                )
            }
            entry<DetailsKey> { key ->
                DetailsScreen(
                    id = key.id,
                    onBack = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}
