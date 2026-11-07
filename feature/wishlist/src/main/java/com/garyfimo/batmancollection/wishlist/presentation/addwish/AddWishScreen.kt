package com.garyfimo.batmancollection.wishlist.presentation.addwish

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.garyfimo.batmancollection.core.domain.FigureType
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWishScreen(
    onBack: () -> Unit,
    onSubmitted: () -> Unit = onBack,
    viewModel: AddWishViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AddWishEffect.Saved -> {
                    snackbarHostState.showSnackbar("Deseo guardado")
                    onSubmitted()
                }
                is AddWishEffect.SubmitFailed ->
                    snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add wish") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag(TAG_ADD_WISH_BACK)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = { viewModel.onIntent(AddWishIntent.Submitted) },
                    enabled = state.isValid && !state.submitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .testTag(TAG_ADD_WISH_SUBMIT),
                ) {
                    Text(if (state.submitting) "Submitting..." else "Submit")
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .testTag(TAG_ADD_WISH_ROOT),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TipoDropdown(
                selected = state.tipo,
                errorText = state.tipoError,
                onSelected = { viewModel.onIntent(AddWishIntent.TipoChanged(it)) },
            )

            RequiredTextField(
                label = "Nombre",
                value = state.nombre,
                errorText = state.nombreError,
                testTag = TAG_ADD_WISH_NOMBRE,
                onValueChange = { viewModel.onIntent(AddWishIntent.NombreChanged(it)) },
            )

            RequiredTextField(
                label = "Serie",
                value = state.serie,
                errorText = state.serieError,
                testTag = TAG_ADD_WISH_SERIE,
                onValueChange = { viewModel.onIntent(AddWishIntent.SerieChanged(it)) },
            )

            OutlinedTextField(
                value = state.url,
                onValueChange = { viewModel.onIntent(AddWishIntent.UrlChanged(it)) },
                label = { Text("URL *") },
                placeholder = { Text("https://…") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                isError = state.urlError != null,
                supportingText = { state.urlError?.let { Text(it) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TAG_ADD_WISH_URL),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TipoDropdown(
    selected: FigureType?,
    errorText: String?,
    onSelected: (FigureType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selected?.label.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Tipo *") },
            placeholder = { Text("Choose") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            isError = errorText != null,
            supportingText = { errorText?.let { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .testTag(TAG_ADD_WISH_TIPO),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            FigureType.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                    modifier = Modifier.testTag("$TAG_ADD_WISH_TIPO_OPTION_PREFIX${option.name}"),
                )
            }
        }
    }
}

@Composable
private fun RequiredTextField(
    label: String,
    value: String,
    errorText: String?,
    testTag: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("$label *") },
        placeholder = { Text("Your answer") },
        singleLine = true,
        isError = errorText != null,
        supportingText = { errorText?.let { Text(it) } },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
    )
}

const val TAG_ADD_WISH_ROOT = "add_wish.root"
const val TAG_ADD_WISH_BACK = "add_wish.back"
const val TAG_ADD_WISH_TIPO = "add_wish.tipo"
const val TAG_ADD_WISH_TIPO_OPTION_PREFIX = "add_wish.tipo.option."
const val TAG_ADD_WISH_NOMBRE = "add_wish.nombre"
const val TAG_ADD_WISH_SERIE = "add_wish.serie"
const val TAG_ADD_WISH_URL = "add_wish.url"
const val TAG_ADD_WISH_SUBMIT = "add_wish.submit"
