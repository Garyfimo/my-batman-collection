package com.garyfimo.batmancollection.wishlist.domain

import com.garyfimo.batmancollection.login.domain.AUTHORIZED_AUTHOR_EMAIL
import com.garyfimo.batmancollection.login.domain.AuthState

/**
 * Only the one authorized owner may add or delete wishes — matches the Firestore
 * rules that gate wishlist writes on `request.auth.token.email`.
 */
fun AuthState.canAddWishes(): Boolean =
    this is AuthState.SignedIn && email.equals(AUTHORIZED_AUTHOR_EMAIL, ignoreCase = true)
