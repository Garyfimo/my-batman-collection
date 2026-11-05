package com.garyfimo.batmancollection.figure.presentation.addfigure

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.garyfimo.batmancollection.analytics.domain.Analytics
import com.garyfimo.batmancollection.figure.domain.FigureAddResult
import com.garyfimo.batmancollection.figure.domain.FigureDraft
import com.garyfimo.batmancollection.figure.domain.FigureRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class AddFigureViewModel(
    private val repository: FigureRepository,
    analytics: Analytics,
) : ViewModel() {

    private val analyticEvents = AddFigureAnalyticEvents(analytics)

    private val _state = MutableStateFlow(AddFigureFormState())
    val state: StateFlow<AddFigureFormState> = _state.asStateFlow()

    private val _effects = Channel<AddFigureEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        analyticEvents.onScreenView()
    }

    fun onIntent(intent: AddFigureIntent) {
        when (intent) {
            is AddFigureIntent.TipoChanged -> _state.value = _state.value.copy(tipo = intent.tipo)
            is AddFigureIntent.NombreChanged -> _state.value = _state.value.copy(nombre = intent.value)
            is AddFigureIntent.SerieChanged -> _state.value = _state.value.copy(serie = intent.value)
            is AddFigureIntent.ImagePicked -> _state.value = _state.value.copy(imageUri = intent.uri)
            AddFigureIntent.Submitted -> submit()
        }
    }

    private fun submit() {
        val current = _state.value.copy(submitAttempted = true)
        _state.value = current
        if (!current.isValid) return

        analyticEvents.onSubmitted()
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
                    analyticEvents.onItemAdded(draft)
                    _effects.send(AddFigureEffect.Saved)
                }
                is FigureAddResult.Failure -> {
                    _state.value = current.copy(submitting = false)
                    val message = result.cause.message ?: "Failed to save"
                    analyticEvents.onItemAddFailed(
                        tipo = draft.tipo,
                        nombre = draft.nombre,
                        serie = draft.serie,
                        error = message,
                    )
                    _effects.send(AddFigureEffect.SubmitFailed(message))
                }
            }
        }
    }
}
