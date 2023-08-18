package com.panoslice.meldphotocollage.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.panoslice.meldphotocollage.data.db.converters.Converters
import com.panoslice.meldphotocollage.data.db.dao.ProjectDao
import com.panoslice.meldphotocollage.data.db.model.ProjectEntity


@Database(
    entities = [ProjectEntity::class],
    version = 1, exportSchema = false
)

@TypeConverters(Converters::class)
abstract class MeldDatabase : RoomDatabase() {
    abstract val projectDao: ProjectDao
}