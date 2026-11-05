package com.garyfimo.batmancollection.login.domain

import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val state: StateFlow<AuthState>
    suspend fun signIn(email: String, password: String): SignInResult
    fun signOut()
}
