package com.garyfimo.batmancollection.login.data

import com.garyfimo.batmancollection.login.domain.AuthRepository
import com.garyfimo.batmancollection.login.domain.AuthState
import com.garyfimo.batmancollection.login.domain.SessionStore
import com.garyfimo.batmancollection.login.domain.SignInResult
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val sessionStore: SessionStore,
    private val maxSessionAgeMillis: Long,
    private val clock: () -> Long = System::currentTimeMillis,
) : AuthRepository {

    // Enforce before snapshotting state so a stale session is cleared in the same tick
    // the repo is constructed — no observer ever sees an expired SignedIn.
    init {
        enforceSessionAge()
    }

    private val _state = MutableStateFlow(auth.toState())
    override val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        auth.addAuthStateListener { _state.value = it.toState() }
    }

    override suspend fun signIn(email: String, password: String): SignInResult = try {
        auth.signInWithEmailAndPassword(email, password).await()
        sessionStore.recordSignIn(clock())
        SignInResult.Success
    } catch (t: Throwable) {
        SignInResult.Failure(t.message ?: "Sign-in failed")
    }

    override fun signOut() {
        auth.signOut()
        sessionStore.clear()
    }

    private fun enforceSessionAge() {
        val user = auth.currentUser
        if (user == null) {
            // No persisted Firebase session → make sure we don't carry a stale timestamp
            // from a prior user who signed out through a path we didn't see.
            sessionStore.clear()
            return
        }
        val signInAt = sessionStore.signInAt()
        if (signInAt == null) {
            // Firebase persisted a session but we have no timestamp: either this is the
            // first launch after shipping the expiry feature, or the user cleared app
            // data for us but not for Firebase. Treat NOW as the sign-in moment so a
            // legitimate signed-in user keeps a full window instead of being bounced.
            sessionStore.recordSignIn(clock())
            return
        }
        if (clock() - signInAt >= maxSessionAgeMillis) {
            auth.signOut()
            sessionStore.clear()
        }
    }

    private fun FirebaseAuth.toState(): AuthState {
        val email = currentUser?.email
        return if (email == null) AuthState.SignedOut else AuthState.SignedIn(email)
    }
}
