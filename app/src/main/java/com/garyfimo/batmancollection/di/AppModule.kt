package com.garyfimo.batmancollection.di

import com.garyfimo.batmancollection.data.FigureRepository
import com.garyfimo.batmancollection.data.FirestoreFigureRepository
import com.garyfimo.batmancollection.ui.addfigure.AddFigureViewModel
import com.garyfimo.batmancollection.ui.details.DetailsViewModel
import com.garyfimo.batmancollection.ui.home.HomeViewModel
import com.google.firebase.firestore.FirebaseFirestore
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

private const val FIRESTORE_DATABASE_ID = "my-batman-collection"

val appModule = module {
    single { FirebaseFirestore.getInstance(FIRESTORE_DATABASE_ID) }
    single<FigureRepository> { FirestoreFigureRepository(get()) }
    viewModelOf(::HomeViewModel)
    viewModel { (id: String) -> DetailsViewModel(id = id, repository = get()) }
    viewModelOf(::AddFigureViewModel)
}
