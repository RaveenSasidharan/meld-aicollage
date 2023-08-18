package com.panoslice.meldphotocollage.di

import com.panoslice.meldphotocollage.ui.home.HomeViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel {
        HomeViewModel(repository = get())
    }
}