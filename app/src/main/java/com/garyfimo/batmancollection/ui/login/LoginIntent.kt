package com.garyfimo.batmancollection.ui.login

sealed interface LoginIntent {
    data class EmailChanged(val value: String) : LoginIntent
    data class PasswordChanged(val value: String) : LoginIntent
    data object Submitted : LoginIntent
}
