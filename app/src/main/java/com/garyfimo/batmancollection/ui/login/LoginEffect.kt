package com.garyfimo.batmancollection.ui.login

sealed interface LoginEffect {
    data object SignedIn : LoginEffect
}
