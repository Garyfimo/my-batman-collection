package com.garyfimo.batmancollection.di

import com.garyfimo.batmancollection.data.BatmanRepository
import com.garyfimo.batmancollection.ui.home.HomeViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    singleOf(::BatmanRepository)
    viewModelOf(::HomeViewModel)
}
