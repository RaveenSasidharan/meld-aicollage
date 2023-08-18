package com.panoslice.meldphotocollage.di

import android.content.Context
import com.panoslice.meldphotocollage.data.MeldRepository
import com.panoslice.meldphotocollage.data.MeldRepositoryImpl
import com.panoslice.meldphotocollage.data.db.dao.ProjectDao
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module


val repositoryModule = module {

    fun provideMeldRepository(context: Context, projectDao: ProjectDao): MeldRepository {
        return MeldRepositoryImpl( context, projectDao)
    }
    single { provideMeldRepository(androidContext(), get()) }

}