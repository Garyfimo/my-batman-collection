package com.garyfimo.batmancollection.di

import android.content.Context
import com.garyfimo.batmancollection.data.AuthRepository
import com.garyfimo.batmancollection.data.FigureRepository
import com.garyfimo.batmancollection.data.FirebaseAuthRepository
import com.garyfimo.batmancollection.data.FirestoreFigureRepository
import com.garyfimo.batmancollection.data.SessionStore
import com.garyfimo.batmancollection.data.SharedPreferencesSessionStore
import com.garyfimo.batmancollection.ui.addfigure.AddFigureViewModel
import com.garyfimo.batmancollection.ui.details.DetailsViewModel
import com.garyfimo.batmancollection.ui.home.HomeViewModel
import com.garyfimo.batmancollection.ui.login.LoginViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

private const val FIRESTORE_DATABASE_ID = "my-batman-collection"
private val SESSION_MAX_AGE_MILLIS: Long = TimeUnit.DAYS.toMillis(30)

val appModule = module {
    single { FirebaseFirestore.getInstance(FIRESTORE_DATABASE_ID) }
    single { FirebaseAuth.getInstance() }
    single { FirebaseStorage.getInstance() }
    single<FigureRepository> { FirestoreFigureRepository(get(), get()) }
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
    viewModelOf(::HomeViewModel)
    viewModel { (id: String) -> DetailsViewModel(id = id, repository = get()) }
    viewModelOf(::AddFigureViewModel)
    viewModelOf(::LoginViewModel)
}
