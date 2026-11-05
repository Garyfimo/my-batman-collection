package com.garyfimo.batmancollection.login.domain

sealed interface SignInResult {
    data object Success : SignInResult
    data class Failure(val message: String) : SignInResult
}
