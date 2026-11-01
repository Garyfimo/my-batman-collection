package com.garyfimo.batmancollection.di

import com.garyfimo.batmancollection.data.BatmanRepository
import com.garyfimo.batmancollection.data.MockBatmanRepository
import com.garyfimo.batmancollection.ui.details.DetailsViewModel
import com.garyfimo.batmancollection.ui.home.HomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {
    single { MockBatmanRepository() } bind BatmanRepository::class
    viewModelOf(::HomeViewModel)
    viewModel { (id: String) -> DetailsViewModel(id = id, repository = get()) }
}
