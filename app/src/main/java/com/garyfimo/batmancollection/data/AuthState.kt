package com.garyfimo.batmancollection.data

sealed interface AuthState {
    data object SignedOut : AuthState
    data class SignedIn(val email: String) : AuthState
}
