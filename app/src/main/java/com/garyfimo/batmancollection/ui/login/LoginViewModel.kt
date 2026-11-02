package com.garyfimo.batmancollection.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.garyfimo.batmancollection.data.AuthRepository
import com.garyfimo.batmancollection.data.SignInResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val submitting: Boolean = false,
    val error: String? = null,
) {
    val isValid: Boolean get() =
        email.isNotBlank() && password.isNotBlank() && !submitting
}

sealed interface LoginEvent {
    data object SignedIn : LoginEvent
}

class LoginViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun setEmail(value: String) {
        _state.value = _state.value.copy(email = value, error = null)
    }

    fun setPassword(value: String) {
        _state.value = _state.value.copy(password = value, error = null)
    }

    fun submit() {
        val current = _state.value
        if (!current.isValid) return
        viewModelScope.launch {
            _state.value = current.copy(submitting = true, error = null)
            when (val result = authRepository.signIn(current.email.trim(), current.password)) {
                SignInResult.Success -> {
                    _state.value = LoginUiState()
                    _events.send(LoginEvent.SignedIn)
                }
                is SignInResult.Failure -> {
                    _state.value = current.copy(submitting = false, error = result.message)
                }
            }
        }
    }
}
