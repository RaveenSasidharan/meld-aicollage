package com.panoslice.meldphotocollage.di

import android.app.Application
import androidx.room.Room
import com.panoslice.meldphotocollage.data.db.MeldDatabase
import com.panoslice.meldphotocollage.data.db.dao.ProjectDao
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val databaseModule = module {

    fun provideDatabase(application: Application): MeldDatabase {
        return Room.databaseBuilder(application, MeldDatabase::class.java, "meld_db")
            .fallbackToDestructiveMigration()
            .build()
    }

    fun provideProjectDao(database: MeldDatabase): ProjectDao {
        return  database.projectDao
    }

    single { provideDatabase(androidApplication()) }
    single { provideProjectDao(get()) }


}