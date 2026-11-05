package com.garyfimo.batmancollection.login.di

import android.content.Context
import com.garyfimo.batmancollection.login.data.FirebaseAuthRepository
import com.garyfimo.batmancollection.login.data.SharedPreferencesSessionStore
import com.garyfimo.batmancollection.login.domain.AuthRepository
import com.garyfimo.batmancollection.login.domain.SessionStore
import com.garyfimo.batmancollection.login.presentation.LoginViewModel
import com.google.firebase.auth.FirebaseAuth
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

private val SESSION_MAX_AGE_MILLIS: Long = TimeUnit.DAYS.toMillis(30)

val loginModule = module {
    single { FirebaseAuth.getInstance() }
    single<SessionStore> {
        SharedPreferencesSessionStore(
            get<Context>().getSharedPreferences(
                SharedPreferencesSessionStore.PREFS_NAME,
                Context.MODE_PRIVATE,
            ),
        )
    }
    single<AuthRepository> {
        FirebaseAuthRepository(
            auth = get(),
            sessionStore = get(),
            maxSessionAgeMillis = SESSION_MAX_AGE_MILLIS,
        )
    }
    viewModelOf(::LoginViewModel)
}
