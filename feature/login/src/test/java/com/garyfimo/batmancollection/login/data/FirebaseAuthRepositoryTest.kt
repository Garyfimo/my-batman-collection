package com.garyfimo.batmancollection.login.data

import com.garyfimo.batmancollection.login.domain.SessionStore
import com.garyfimo.batmancollection.login.domain.SignInResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.concurrent.TimeUnit

class FirebaseAuthRepositoryTest {

    private class FakeSessionStore(private var value: Long? = null) : SessionStore {
        override fun signInAt(): Long? = value
        override fun recordSignIn(epochMillis: Long) { value = epochMillis }
        override fun clear() { value = null }
    }

    private fun authMock(signedInEmail: String? = null): FirebaseAuth {
        val mock = mockk<FirebaseAuth>()
        val user = signedInEmail?.let {
            mockk<FirebaseUser>().also { u -> every { u.email } returns it }
        }
        every { mock.currentUser } returns user
        every { mock.signOut() } just runs
        every { mock.addAuthStateListener(any()) } just runs
        return mock
    }

    private val thirtyDays = TimeUnit.DAYS.toMillis(30)
    private val now = 1_700_000_000_000L

    @Test
    fun `signed-out user leaves no stored timestamp`() {
        val store = FakeSessionStore(value = 123L) // leftover from a prior user
        val auth = authMock(signedInEmail = null)

        FirebaseAuthRepository(auth, store, thirtyDays) { now }

        assertNull(store.signInAt())
        verify(exactly = 0) { auth.signOut() }
    }

    @Test
    fun `signed-in user without a timestamp gets one recorded, no sign-out`() {
        val store = FakeSessionStore(value = null)
        val auth = authMock(signedInEmail = "garyfimo@gmail.com")

        FirebaseAuthRepository(auth, store, thirtyDays) { now }

        assertEquals(now, store.signInAt())
        verify(exactly = 0) { auth.signOut() }
    }

    @Test
    fun `fresh session is left alone`() {
        val store = FakeSessionStore(value = now - TimeUnit.DAYS.toMillis(5))
        val auth = authMock(signedInEmail = "garyfimo@gmail.com")

        FirebaseAuthRepository(auth, store, thirtyDays) { now }

        assertEquals(now - TimeUnit.DAYS.toMillis(5), store.signInAt())
        verify(exactly = 0) { auth.signOut() }
    }

    @Test
    fun `session at exactly the max age is expired`() {
        val store = FakeSessionStore(value = now - thirtyDays)
        val auth = authMock(signedInEmail = "garyfimo@gmail.com")

        FirebaseAuthRepository(auth, store, thirtyDays) { now }

        verify(exactly = 1) { auth.signOut() }
        assertNull(store.signInAt())
    }

    @Test
    fun `older session is expired and store cleared`() {
        val store = FakeSessionStore(value = now - TimeUnit.DAYS.toMillis(31))
        val auth = authMock(signedInEmail = "garyfimo@gmail.com")

        FirebaseAuthRepository(auth, store, thirtyDays) { now }

        verify(exactly = 1) { auth.signOut() }
        assertNull(store.signInAt())
    }

    @Test
    fun `signOut clears both Firebase and the stored timestamp`() {
        val store = FakeSessionStore(value = now)
        val auth = authMock(signedInEmail = "garyfimo@gmail.com")
        val repo = FirebaseAuthRepository(auth, store, thirtyDays) { now }

        repo.signOut()

        verify(exactly = 1) { auth.signOut() }
        assertNull(store.signInAt())
    }
}
