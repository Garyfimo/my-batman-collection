package com.garyfimo.batmancollection.figure.presentation.addfigure

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.garyfimo.batmancollection.core.domain.FigureType
import java.io.File
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFigureScreen(
    onBack: () -> Unit,
    onSubmitted: () -> Unit = onBack,
    viewModel: AddFigureViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AddFigureEffect.Saved -> {
                    snackbarHostState.showSnackbar("Figura guardada")
                    onSubmitted()
                }
                is AddFigureEffect.SubmitFailed ->
                    snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add figure") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag(TAG_ADD_BACK)) {
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
                    onClick = { viewModel.onIntent(AddFigureIntent.Submitted) },
                    enabled = state.isValid && !state.submitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .testTag(TAG_ADD_SUBMIT),
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
                .testTag(TAG_ADD_ROOT),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TipoDropdown(
                selected = state.tipo,
                errorText = state.tipoError,
                onSelected = { viewModel.onIntent(AddFigureIntent.TipoChanged(it)) },
            )

            RequiredTextField(
                label = "Nombre",
                value = state.nombre,
                errorText = state.nombreError,
                testTag = TAG_ADD_NOMBRE,
                onValueChange = { viewModel.onIntent(AddFigureIntent.NombreChanged(it)) },
            )

            RequiredTextField(
                label = "Serie",
                value = state.serie,
                errorText = state.serieError,
                testTag = TAG_ADD_SERIE,
                onValueChange = { viewModel.onIntent(AddFigureIntent.SerieChanged(it)) },
            )

            ImagePickerField(
                imageUri = state.imageUri,
                errorText = state.imageError,
                onImagePicked = { viewModel.onIntent(AddFigureIntent.ImagePicked(it)) },
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
                .testTag(TAG_ADD_TIPO),
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
                    modifier = Modifier.testTag("$TAG_ADD_TIPO_OPTION_PREFIX${option.name}"),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImagePickerField(
    imageUri: String?,
    errorText: String?,
    onImagePicked: (String?) -> Unit,
) {
    val context = LocalContext.current
    var sheetOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    // Survives config changes so if the system kills the activity while the
    // camera app is foregrounded we still know where the capture was supposed
    // to land when we come back.
    var pendingCameraUri by rememberSaveable { mutableStateOf<String?>(null) }

    val gallery = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> onImagePicked(uri?.toString()) }

    val camera = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success) onImagePicked(pendingCameraUri)
        pendingCameraUri = null
    }

    fun closeSheet(then: () -> Unit = {}) {
        scope.launch {
            sheetState.hide()
            sheetOpen = false
            then()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Imagen referencial *", style = MaterialTheme.typography.bodyLarge)
        Text(
            "Upload 1 supported file: image. Max 10 MB.",
            style = MaterialTheme.typography.bodySmall,
        )

        if (imageUri != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(12.dp))
                    .testTag(TAG_ADD_IMAGE_PREVIEW),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Selected image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        OutlinedButton(
            onClick = { sheetOpen = true },
            modifier = Modifier.testTag(TAG_ADD_ADD_FILE),
        ) {
            Text(if (imageUri == null) "Add file" else "Replace file")
        }

        if (errorText != null) {
            Text(
                text = errorText,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    if (sheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            sheetState = sheetState,
            modifier = Modifier.testTag(TAG_ADD_IMAGE_SHEET),
        ) {
            ListItem(
                headlineContent = { Text("Take photo") },
                leadingContent = {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        closeSheet {
                            val uri = newCameraUri(context)
                            pendingCameraUri = uri.toString()
                            camera.launch(uri)
                        }
                    }
                    .testTag(TAG_ADD_IMAGE_CAMERA),
            )
            ListItem(
                headlineContent = { Text("Choose from gallery") },
                leadingContent = {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        closeSheet {
                            gallery.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly,
                                ),
                            )
                        }
                    }
                    .testTag(TAG_ADD_IMAGE_GALLERY),
            )
        }
    }
}

private fun newCameraUri(context: Context): Uri {
    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File.createTempFile("capture_", ".jpg", dir)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
}

const val TAG_ADD_ROOT = "add.root"
const val TAG_ADD_BACK = "add.back"
const val TAG_ADD_TIPO = "add.tipo"
const val TAG_ADD_TIPO_OPTION_PREFIX = "add.tipo.option."
const val TAG_ADD_NOMBRE = "add.nombre"
const val TAG_ADD_SERIE = "add.serie"
const val TAG_ADD_IMAGE_PREVIEW = "add.image.preview"
const val TAG_ADD_ADD_FILE = "add.image.add"
const val TAG_ADD_IMAGE_SHEET = "add.image.sheet"
const val TAG_ADD_IMAGE_CAMERA = "add.image.camera"
const val TAG_ADD_IMAGE_GALLERY = "add.image.gallery"
const val TAG_ADD_SUBMIT = "add.submit"
