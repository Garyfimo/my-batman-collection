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

class LoginViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _effects = Channel<LoginEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.EmailChanged ->
                _state.value = _state.value.copy(email = intent.value, error = null)
            is LoginIntent.PasswordChanged ->
                _state.value = _state.value.copy(password = intent.value, error = null)
            LoginIntent.Submitted -> submit()
        }
    }

    private fun submit() {
        val current = _state.value
        if (!current.isValid) return
        viewModelScope.launch {
            _state.value = current.copy(submitting = true, error = null)
            when (val result = authRepository.signIn(current.email.trim(), current.password)) {
                SignInResult.Success -> {
                    _state.value = LoginUiState()
                    _effects.send(LoginEffect.SignedIn)
                }
                is SignInResult.Failure -> {
                    _state.value = current.copy(submitting = false, error = result.message)
                }
            }
        }
    }
}
