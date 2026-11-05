package com.garyfimo.batmancollection.login.presentation

sealed interface LoginEffect {
    data object SignedIn : LoginEffect
}
