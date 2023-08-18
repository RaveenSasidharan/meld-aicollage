package com.panoslice.meldphotocollage.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.panoslice.meldphotocollage.data.db.model.ProjectEntity

@Dao
interface ProjectDao {

    @Query("SELECT * FROM projects")
    fun findAll(): List<ProjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(project: ProjectEntity)
}