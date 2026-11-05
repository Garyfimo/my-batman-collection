package com.garyfimo.batmancollection.ui.login

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val submitting: Boolean = false,
    val error: String? = null,
) {
    val isValid: Boolean get() =
        email.isNotBlank() && password.isNotBlank() && !submitting
}
