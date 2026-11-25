package com.garyfimo.batmancollection.settings.di

import com.garyfimo.batmancollection.settings.presentation.SettingsAnalyticEvents
import com.garyfimo.batmancollection.settings.presentation.SettingsViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val settingsModule = module {
    factoryOf(::SettingsAnalyticEvents)
    viewModelOf(::SettingsViewModel)
}
