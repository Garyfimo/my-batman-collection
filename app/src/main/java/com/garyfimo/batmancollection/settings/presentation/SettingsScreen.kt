package com.garyfimo.batmancollection.settings.presentation

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.garyfimo.batmancollection.ui.theme.BatmanPalette
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag(TAG_SETTINGS_BACK),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).testTag(TAG_SETTINGS_ROOT)) {
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            ListItem(
                headlineContent = { Text("Classic") },
                supportingContent = { Text("Midnight blue and bat-yellow") },
                trailingContent = {
                    RadioButton(
                        selected = state.selected == BatmanPalette.CLASSIC,
                        onClick = null,
                        modifier = Modifier
                            .testTag(TAG_SETTINGS_RADIO_CLASSIC)
                            .semantics { selected = state.selected == BatmanPalette.CLASSIC },
                    )
                },
                modifier = Modifier
                    .selectable(
                        selected = state.selected == BatmanPalette.CLASSIC,
                        role = Role.RadioButton,
                        onClick = {
                            viewModel.onIntent(SettingsIntent.PaletteSelected(BatmanPalette.CLASSIC))
                        },
                    )
                    .testTag(TAG_SETTINGS_ROW_CLASSIC),
            )
            ListItem(
                headlineContent = { Text("Noir") },
                supportingContent = { Text("Monochrome with a cold accent") },
                trailingContent = {
                    RadioButton(
                        selected = state.selected == BatmanPalette.NOIR,
                        onClick = null,
                        modifier = Modifier
                            .testTag(TAG_SETTINGS_RADIO_NOIR)
                            .semantics { selected = state.selected == BatmanPalette.NOIR },
                    )
                },
                modifier = Modifier
                    .selectable(
                        selected = state.selected == BatmanPalette.NOIR,
                        role = Role.RadioButton,
                        onClick = {
                            viewModel.onIntent(SettingsIntent.PaletteSelected(BatmanPalette.NOIR))
                        },
                    )
                    .testTag(TAG_SETTINGS_ROW_NOIR),
            )
            ListItem(
                headlineContent = { Text("Dark Knight") },
                supportingContent = { Text("Bat-yellow on pure black") },
                trailingContent = {
                    RadioButton(
                        selected = state.selected == BatmanPalette.DARK_KNIGHT,
                        onClick = null,
                        modifier = Modifier
                            .testTag(TAG_SETTINGS_RADIO_DARK_KNIGHT)
                            .semantics { selected = state.selected == BatmanPalette.DARK_KNIGHT },
                    )
                },
                modifier = Modifier
                    .selectable(
                        selected = state.selected == BatmanPalette.DARK_KNIGHT,
                        role = Role.RadioButton,
                        onClick = {
                            viewModel.onIntent(SettingsIntent.PaletteSelected(BatmanPalette.DARK_KNIGHT))
                        },
                    )
                    .testTag(TAG_SETTINGS_ROW_DARK_KNIGHT),
            )
        }
    }
}

const val TAG_SETTINGS_ROOT = "settings.root"
const val TAG_SETTINGS_BACK = "settings.back"
const val TAG_SETTINGS_ROW_CLASSIC = "settings.row.classic"
const val TAG_SETTINGS_ROW_NOIR = "settings.row.noir"
const val TAG_SETTINGS_ROW_DARK_KNIGHT = "settings.row.dark_knight"
const val TAG_SETTINGS_RADIO_CLASSIC = "settings.radio.classic"
const val TAG_SETTINGS_RADIO_NOIR = "settings.radio.noir"
const val TAG_SETTINGS_RADIO_DARK_KNIGHT = "settings.radio.dark_knight"
