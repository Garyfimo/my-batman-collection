package com.garyfimo.batmancollection.login.domain

interface SessionStore {
    /** Epoch millis of the current sign-in, or null if no session is tracked. */
    fun signInAt(): Long?
    fun recordSignIn(epochMillis: Long)
    fun clear()
}
