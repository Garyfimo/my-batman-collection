package com.garyfimo.batmancollection.ui.addfigure

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.garyfimo.batmancollection.data.FigureAddResult
import com.garyfimo.batmancollection.data.FigureDraft
import com.garyfimo.batmancollection.data.FigureRepository
import com.garyfimo.batmancollection.data.FigureType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class AddFigureFormState(
    val tipo: FigureType? = null,
    val nombre: String = "",
    val serie: String = "",
    val imageUri: String? = null,
    val submitAttempted: Boolean = false,
    val submitting: Boolean = false,
) {
    val tipoError: String? get() =
        if (submitAttempted && tipo == null) ERROR_REQUIRED else null
    val nombreError: String? get() =
        if (submitAttempted && nombre.isBlank()) ERROR_REQUIRED else null
    val serieError: String? get() =
        if (submitAttempted && serie.isBlank()) ERROR_REQUIRED else null
    val imageError: String? get() =
        if (submitAttempted && imageUri.isNullOrBlank()) ERROR_REQUIRED else null

    val isValid: Boolean get() =
        tipo != null && nombre.isNotBlank() && serie.isNotBlank() && !imageUri.isNullOrBlank()

    companion object {
        const val ERROR_REQUIRED = "This is a required question"
    }
}

sealed interface AddFigureEvent {
    data object Submitted : AddFigureEvent
    data class SubmitFailed(val message: String) : AddFigureEvent
}

class AddFigureViewModel(
    private val repository: FigureRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AddFigureFormState())
    val state: StateFlow<AddFigureFormState> = _state.asStateFlow()

    private val _events = Channel<AddFigureEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun setTipo(tipo: FigureType) {
        _state.value = _state.value.copy(tipo = tipo)
    }

    fun setNombre(value: String) {
        _state.value = _state.value.copy(nombre = value)
    }

    fun setSerie(value: String) {
        _state.value = _state.value.copy(serie = value)
    }

    fun setImageUri(uri: String?) {
        _state.value = _state.value.copy(imageUri = uri)
    }

    fun submit() {
        val current = _state.value.copy(submitAttempted = true)
        _state.value = current
        if (!current.isValid) return

        viewModelScope.launch {
            _state.value = current.copy(submitting = true)
            val draft = FigureDraft(
                tipo = current.tipo!!,
                nombre = current.nombre.trim(),
                serie = current.serie.trim(),
                imageUri = current.imageUri!!,
            )
            when (val result = repository.add(draft)) {
                is FigureAddResult.Success -> {
                    _state.value = AddFigureFormState()
                    _events.send(AddFigureEvent.Submitted)
                }
                is FigureAddResult.Failure -> {
                    _state.value = current.copy(submitting = false)
                    _events.send(
                        AddFigureEvent.SubmitFailed(result.cause.message ?: "Failed to save"),
                    )
                }
            }
        }
    }
}
