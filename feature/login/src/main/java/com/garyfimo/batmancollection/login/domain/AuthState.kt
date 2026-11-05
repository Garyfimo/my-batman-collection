package com.garyfimo.batmancollection.login.domain

sealed interface AuthState {
    data object SignedOut : AuthState
    data class SignedIn(val email: String) : AuthState
}
