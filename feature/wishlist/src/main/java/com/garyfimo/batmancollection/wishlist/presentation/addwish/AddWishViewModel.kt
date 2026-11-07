package com.garyfimo.batmancollection.wishlist.presentation.addwish

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.garyfimo.batmancollection.analytics.domain.Analytics
import com.garyfimo.batmancollection.wishlist.domain.WishAddResult
import com.garyfimo.batmancollection.wishlist.domain.WishDraft
import com.garyfimo.batmancollection.wishlist.domain.WishRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class AddWishViewModel(
    private val repository: WishRepository,
    analytics: Analytics,
) : ViewModel() {

    private val analyticEvents = AddWishAnalyticEvents(analytics)

    private val _state = MutableStateFlow(AddWishFormState())
    val state: StateFlow<AddWishFormState> = _state.asStateFlow()

    private val _effects = Channel<AddWishEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        analyticEvents.onScreenView()
    }

    fun onIntent(intent: AddWishIntent) {
        when (intent) {
            is AddWishIntent.TipoChanged -> _state.value = _state.value.copy(tipo = intent.tipo)
            is AddWishIntent.NombreChanged -> _state.value = _state.value.copy(nombre = intent.value)
            is AddWishIntent.SerieChanged -> _state.value = _state.value.copy(serie = intent.value)
            is AddWishIntent.UrlChanged -> _state.value = _state.value.copy(url = intent.value)
            AddWishIntent.Submitted -> submit()
        }
    }

    private fun submit() {
        val current = _state.value.copy(submitAttempted = true)
        _state.value = current
        if (!current.isValid) return

        analyticEvents.onSubmitted()
        viewModelScope.launch {
            _state.value = current.copy(submitting = true)
            val draft = WishDraft(
                tipo = current.tipo!!,
                nombre = current.nombre.trim(),
                serie = current.serie.trim(),
                url = current.url.trim(),
            )
            when (val result = repository.add(draft)) {
                is WishAddResult.Success -> {
                    _state.value = AddWishFormState()
                    analyticEvents.onWishAdded(draft)
                    _effects.send(AddWishEffect.Saved)
                }
                is WishAddResult.Failure -> {
                    _state.value = current.copy(submitting = false)
                    val message = result.cause.message ?: "Failed to save"
                    analyticEvents.onWishAddFailed(
                        tipo = draft.tipo,
                        nombre = draft.nombre,
                        serie = draft.serie,
                        error = message,
                    )
                    _effects.send(AddWishEffect.SubmitFailed(message))
                }
            }
        }
    }
}
