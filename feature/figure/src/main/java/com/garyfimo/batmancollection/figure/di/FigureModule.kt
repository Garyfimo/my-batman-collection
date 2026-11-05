package com.garyfimo.batmancollection.figure.di

import com.garyfimo.batmancollection.figure.data.FirestoreFigureRepository
import com.garyfimo.batmancollection.figure.domain.FigureRepository
import com.garyfimo.batmancollection.figure.presentation.addfigure.AddFigureViewModel
import com.garyfimo.batmancollection.figure.presentation.details.DetailsViewModel
import com.garyfimo.batmancollection.figure.presentation.home.HomeViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

private const val FIRESTORE_DATABASE_ID = "my-batman-collection"

val figureModule = module {
    single { FirebaseFirestore.getInstance(FIRESTORE_DATABASE_ID) }
    single { FirebaseStorage.getInstance() }
    single<FigureRepository> { FirestoreFigureRepository(get(), get()) }
    viewModelOf(::HomeViewModel)
    viewModel { (id: String) -> DetailsViewModel(id = id, repository = get()) }
    viewModelOf(::AddFigureViewModel)
}
