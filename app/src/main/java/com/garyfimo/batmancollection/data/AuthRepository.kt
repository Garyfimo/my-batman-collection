package com.garyfimo.batmancollection.data

import kotlinx.coroutines.flow.StateFlow

sealed interface SignInResult {
    data object Success : SignInResult
    data class Failure(val message: String) : SignInResult
}

interface AuthRepository {
    val state: StateFlow<AuthState>
    suspend fun signIn(email: String, password: String): SignInResult
    fun signOut()
}
