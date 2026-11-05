package com.garyfimo.batmancollection.login.domain

/**
 * The one email that may add new figures. Mirrors the Firestore / Storage security
 * rules that gate `write` on `request.auth.token.email`.
 */
const val AUTHORIZED_AUTHOR_EMAIL = "garyfimo@gmail.com"

fun AuthState.canAddFigures(): Boolean =
    this is AuthState.SignedIn && email.equals(AUTHORIZED_AUTHOR_EMAIL, ignoreCase = true)
