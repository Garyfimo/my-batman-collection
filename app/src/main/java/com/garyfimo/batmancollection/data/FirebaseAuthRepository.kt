package com.garyfimo.batmancollection.data

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
) : AuthRepository {

    private val _state = MutableStateFlow(auth.toState())
    override val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        auth.addAuthStateListener { _state.value = it.toState() }
    }

    override suspend fun signIn(email: String, password: String): SignInResult = try {
        auth.signInWithEmailAndPassword(email, password).await()
        SignInResult.Success
    } catch (t: Throwable) {
        SignInResult.Failure(t.message ?: "Sign-in failed")
    }

    override fun signOut() {
        auth.signOut()
    }

    private fun FirebaseAuth.toState(): AuthState {
        val email = currentUser?.email
        return if (email == null) AuthState.SignedOut else AuthState.SignedIn(email)
    }
}
