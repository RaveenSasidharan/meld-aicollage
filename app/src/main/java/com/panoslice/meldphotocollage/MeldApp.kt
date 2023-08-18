package com.panoslice.meldphotocollage

import android.app.Application
import com.panoslice.meldphotocollage.di.databaseModule
import com.panoslice.meldphotocollage.di.networkModule
import com.panoslice.meldphotocollage.di.repositoryModule
import com.panoslice.meldphotocollage.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MeldApp : Application(){
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@MeldApp)
            modules(
                viewModelModule,
                repositoryModule,
                networkModule,
                databaseModule
            )
        }
    }
}