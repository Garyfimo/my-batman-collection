package com.garyfimo.batmancollection.login.presentation

sealed interface LoginIntent {
    data class EmailChanged(val value: String) : LoginIntent
    data class PasswordChanged(val value: String) : LoginIntent
    data object Submitted : LoginIntent
}
