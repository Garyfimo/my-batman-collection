package com.garyfimo.batmancollection.login.presentation

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val submitting: Boolean = false,
    val error: String? = null,
) {
    val isValid: Boolean get() =
        email.isNotBlank() && password.isNotBlank() && !submitting
}
